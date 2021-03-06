package org.nohope.protobuf.rpc.client;

import com.google.protobuf.*;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.nohope.logging.Logger;
import org.nohope.logging.LoggerFactory;
import org.nohope.protobuf.core.Controller;
import org.nohope.protobuf.core.MessageUtils;
import org.nohope.protobuf.core.exception.DetailedExpectedException;
import org.nohope.protobuf.core.exception.RpcTimeoutException;
import org.nohope.protobuf.core.exception.UnexpectedServiceException;
import org.nohope.rpc.protocol.RPC;
import org.nohope.rpc.protocol.RPC.RpcRequest;
import org.nohope.rpc.protocol.RPC.RpcRequest.Builder;
import org.nohope.rpc.protocol.RPC.RpcResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.protobuf.Descriptors.MethodDescriptor;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">ketoth xupack</a>
 * @since 2013-10-01 15:09
 */
class RpcChannelImpl implements RpcChannel, BlockingRpcChannel {
    private static final Logger LOG = LoggerFactory.getLogger(RpcChannelImpl.class);

    //private final Channel channel;
    private final RpcClientHandler handler;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<String, ExtensionRegistry> extensionsCache = new ConcurrentHashMap<>();

    private final long timeout;
    private final TimeUnit unit;

    private final AtomicReference<Channel> channel = new AtomicReference<>();
    private final ClientBootstrap bootstrap;

    RpcChannelImpl(@Nonnull final ClientBootstrap bootstrap,
                   final long timeout,
                   @Nonnull final TimeUnit timeoutUnit) {
        final Channel unboundChannel = bootstrap.connect().getChannel();

        this.bootstrap = bootstrap;
        this.timeout = timeout;
        this.unit = timeoutUnit;
        this.channel.set(unboundChannel);
        this.handler = this.channel.get().getPipeline().get(RpcClientHandler.class);
        if (handler == null) {
            throw new IllegalArgumentException("Channel does not have proper handler");
        }
    }

    public static Controller newRpcController() {
        return new Controller();
    }

    private void write(@Nonnull final RpcRequest request) throws UnexpectedServiceException {
        getChannel().write(request);
    }

    Channel getChannel() throws UnexpectedServiceException {
        if (!this.channel.get().isConnected()) {
            // FIXME: is awaitUninterruptibly is really necessary here?
            final ChannelFuture connectFuture = bootstrap.connect().awaitUninterruptibly();
            final Throwable cause = connectFuture.getCause();
            if (cause != null) {
                throw new UnexpectedServiceException(cause);
            }
            this.channel.set(connectFuture.getChannel());
        }
        return this.channel.get();
    }

    @Override
    public Message callBlockingMethod(final MethodDescriptor method,
                                      @Nullable final RpcController originController,
                                      final Message request,
                                      final Message responsePrototype) throws ServiceException {

        final Controller controller;
        if (originController == null) {
            controller = newRpcController();
        } else {
            if (!(originController instanceof Controller)) {
                throw new IllegalArgumentException("Invalid controller type. You should RpcChannelImpl.newRpcController()");
            }
            controller = (Controller) originController;
        }

        final BlockingRpcCallback callback = new BlockingRpcCallback();

        final ServiceDescriptor service = method.getService();
        final String serviceFullName = service.getFullName();
        if (!extensionsCache.containsKey(serviceFullName)) {
            final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
            service.getFile().getExtensions().forEach(extensionRegistry::add);
            extensionsCache.put(serviceFullName, extensionRegistry);
        }

        final ResponsePrototypeRpcCallback rpcCallback =
                new ResponsePrototypeRpcCallback(
                        controller,
                        responsePrototype,
                        extensionsCache.get(serviceFullName),
                        callback);

        final int nextSeqId = handler.getNextSeqId();
        final RpcRequest rpcRequest = buildRequest(nextSeqId, method, request);
        handler.registerCallback(nextSeqId, rpcCallback);

        write(rpcRequest);

        final Future<Message> handler = executor.submit(() -> {
            synchronized (callback) {
                while (!callback.isDone()) {
                    callback.wait();
                }
            }

            if (rpcCallback.getRpcResponse() != null && rpcCallback.getRpcResponse().hasError()) {
                final RPC.Error error = rpcCallback.controller.getError();
                if (error != null) {
                    throw new DetailedExpectedException(error);
                }

                // TODO: should we only throw this if the error code matches the
                // case where the server call threw a ServiceException?
                throw new ServiceException(rpcCallback.getRpcResponse().getError().getErrorMessage());
            }
            return callback.getMessage();
        });

        try {
            return handler.get(timeout, unit);
        } catch (final ExecutionException e) {
            // TODO: more exceptional types
            final Throwable cause = e.getCause();
            if (cause instanceof ServiceException) {
                throw (ServiceException) cause;
            }
            throw new IllegalStateException(e);
        } catch (final TimeoutException e) {
            throw new RpcTimeoutException(e);
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RpcRequest buildRequest(final int seqId,
                                           final MethodDescriptor method,
                                           final MessageLite request) {
        final Builder requestBuilder = RpcRequest.newBuilder();
        return requestBuilder
                .setId(seqId)
                .setServiceName(method.getService().getFullName())
                .setMethodName(method.getName())
                .setPayload(request.toByteString())
                .build();
    }

    @Override
    public void callMethod(final MethodDescriptor method,
                           final RpcController controller,
                           final Message request,
                           final Message responsePrototype,
                           final RpcCallback<Message> done) {
        throw new UnsupportedOperationException("TBD");
    }

    static class ResponsePrototypeRpcCallback implements RpcCallback<RpcResponse> {
        private final Controller controller;
        private final Message responsePrototype;
        private final RpcCallback<Message> callback;
        private final ExtensionRegistry extensionRegistry;

        private RpcResponse rpcResponse;

        ResponsePrototypeRpcCallback(@Nonnull final Controller controller,
                                     @Nonnull final Message responsePrototype,
                                     @Nonnull final ExtensionRegistry extensionRegistry,
                                     @Nonnull final RpcCallback<Message> callback) {
            this.controller = controller;
            this.responsePrototype = responsePrototype;
            this.callback = callback;
            this.extensionRegistry = extensionRegistry;
        }

        @Override
        public void run(final RpcResponse message) {
            rpcResponse = message;
            if (rpcResponse == null) {
                callback.run(null);
                return;
            }

            if (rpcResponse.hasError()) {
                try {
                    rpcResponse = MessageUtils.repairedMessage(rpcResponse, extensionRegistry);
                } catch (final InvalidProtocolBufferException e) {
                    LOG.warn("Could not marshall into error message", e);
                }
                controller.setError(rpcResponse.getError());
                callback.run(rpcResponse);
                return;
            }

            try {
                final Message response =
                        responsePrototype.newBuilderForType()
                                .mergeFrom(message.getPayload(), extensionRegistry)
                                .build();
                callback.run(response);
            } catch (final InvalidProtocolBufferException e) {
                LOG.warn("Could not marshall into response", e);
                controller.setFailed("Received invalid response type from server");
                callback.run(null);
            }
        }

        @Nonnull
        public Controller getRpcController() {
            return controller;
        }

        public RpcResponse getRpcResponse() {
            return rpcResponse;
        }
    }

    private static class BlockingRpcCallback implements RpcCallback<Message> {
        private boolean done;
        private Message message;

        @Override
        public void run(final Message message) {
            this.message = message;
            synchronized (this) {
                done = true;
                notify();
            }
        }

        public Message getMessage() {
            return message;
        }

        public boolean isDone() {
            return done;
        }
    }
}
