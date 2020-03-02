import java.io.IOException;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class PsqlStorageSystemsFactory extends StorageSystemFactory<Connection> {
    private static final Logger LOGGER = Logger.getLogger(PsqlStorageSystemsFactory.class.getName());

    public PsqlStorageSystemsFactory(ExecutorService executorService) throws IOException {
        super(executorService, new PsqlSnapshottedWrapper(), Constants.PSQL_LISTEN_PORT);
    }

    @Override
    public JointStorageSystem<Connection> simpleOlep() {
        var ss = new JointStorageSystem<>("PSQL simple olep", loopingKafka, httpStorageSystem, snapshottedWrapper)

            // POST MESSAGE
            .registerService(new ServiceBase<>(StupidStreamObject.ObjectType.POST_MESSAGE, false) {
                @Override
                void handleRequest(StupidStreamObject request,
                                   WrappedSnapshottedStorageSystem<Connection> wrapper,
                                   Consumer<MultithreadedResponse> responseCallback) {
                    wrapper.postMessage(RequestPostMessage.fromStupidStreamObject(request));
                    var response = new MultithreadedResponse(request.getResponseAddress().getChannelID(), null);
                    responseCallback.accept(response);
                }
            })
            // DELETE ALL MESSAGES
            .registerService(new ServiceBase<>(StupidStreamObject.ObjectType.DELETE_ALL_MESSAGES, false) {
                @Override
                void handleRequest(StupidStreamObject request,
                                   WrappedSnapshottedStorageSystem<Connection> wrapper,
                                   Consumer<MultithreadedResponse> responseCallback) {
                    wrapper.deleteAllMessages();
                    var response = new MultithreadedResponse(request.getResponseAddress().getChannelID(), null);
                    responseCallback.accept(response);
                }
            })
            // GET ALL MESSAGES
            .registerService(new ServiceBase<>(StupidStreamObject.ObjectType.GET_ALL_MESSAGES, false) {
                @Override
                void handleRequest(StupidStreamObject request,
                                   WrappedSnapshottedStorageSystem<Connection> wrapper,
                                   Consumer<MultithreadedResponse> responseCallback) {
                    var dbResponse = wrapper.getAllMessages(wrapper.getDefaultSnapshot(),
                        RequestAllMessages.fromStupidStreamObject(request));
                    responseCallback.accept(
                        new MultithreadedResponse(request.getResponseAddress().getChannelID(), dbResponse));
                }
            })
            // GET MESSAGE DETAILS
            .registerService(new ServiceBase<>(StupidStreamObject.ObjectType.GET_MESSAGE_DETAILS, false) {
                @Override
                void handleRequest(StupidStreamObject request,
                                   WrappedSnapshottedStorageSystem<Connection> wrapper,
                                   Consumer<MultithreadedResponse> responseCallback) {
                    var dbResponse = wrapper.getMessageDetails(wrapper.getDefaultSnapshot(),
                        RequestMessageDetails.fromStupidStreamObject(request));
                    responseCallback.accept(
                        new MultithreadedResponse(request.getResponseAddress().getChannelID(), dbResponse)
                    );
                }
            })
            // SEARCH MESSAGE
            .registerService(new ServiceBase<>(StupidStreamObject.ObjectType.SEARCH_MESSAGES, false) {
                @Override
                void handleRequest(StupidStreamObject request,
                                   WrappedSnapshottedStorageSystem<Connection> wrapper,
                                   Consumer<MultithreadedResponse> responseCallback) {
                    LOGGER.info("PSQL simple olep skips search request");
                }
            })
            // NOP
            .registerService(new ServiceBase<>(StupidStreamObject.ObjectType.NOP, false) {
                @Override
                void handleRequest(StupidStreamObject request,
                                   WrappedSnapshottedStorageSystem<Connection> wrapper,
                                   Consumer<MultithreadedResponse> responseCallback) {
                    LOGGER.info("PSQL simple olep received NOP");
//                    responseCallback.accept(new StupidStreamObject(StupidStreamObject.ObjectType.NOP,
//                        Constants.NO_RESPONSE));
                }
            });

        this.executorService.submit(loopingKafka::listenBlockingly);
        return ss;
    }
}
