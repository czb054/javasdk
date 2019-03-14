package cn.hyperchain.sdk.provider;

import cn.hyperchain.sdk.exception.AllNodesBadException;
import cn.hyperchain.sdk.exception.RequestException;
import cn.hyperchain.sdk.request.Request;
import cn.hyperchain.sdk.response.Response;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * ProviderManager负责负载均衡，封装header等
 */
public class ProviderManager {
    public ArrayList<HttpProvider> httpProviders;
    public int requestIndex; // todo: requestIndex需要和Request进行绑定

    private static Logger logger = Logger.getLogger(DefaultHttpProvider.class);

    public ProviderManager(Builder builder) {
        this.httpProviders = builder.httpProviders;
    }

    public String send(Request request, int... ids) throws RequestException {
        // todo: 考虑负载均衡和断线重连
        // todo: 考虑切换节点

        for (int id : ids) {
            if (httpProviders.get(id).getStatus() == PStatus.GOOD) {
                // todo: 将选择好的节点记录到request的 providerIndex变量中，便于重发等操作
//                request.setRequestNode(id);
                try {
                    return sendTo(request, id);
                } catch (RequestException e) {
                    if (e.getCode() == -9999) {
                        logger.debug("send to id: " + id + " failed");
                        continue;
                    }
                    // other exception rethrow
                    throw e;
                    // fixme : we should query all ids again.
                }
            }
        }
        logger.error("All nodes are bad, please check it or wait for reconnecting successfully!");
        throw new AllNodesBadException("No node to connect!");
    }

    private String sendTo(Request request, int id) throws RequestException {
        String body = getRequestBody(request);
        Map<String, String> headers = getHeaders(body);

        // todo: 将选择好的节点记录到request的 providerIndex变量中，便于重发等操作
        return httpProviders.get(id).post(body, headers);
    }

    private String getRequestBody(Request request) {
        return request.requestBody();
    }

    private Map<String, String> getHeaders(String body) {
        return new HashMap<>();
    }

    public <K extends Response> String sendRequest(Request<K> kRequest, int[] nodeIdxs) {
        return null;
    }


    public static class Builder {
        private ArrayList<HttpProvider> httpProviders = new ArrayList<>();
        //todo : add config and more

        public Builder() {

        }

        public Builder setHttpProviders(HttpProvider... providers) {
            this.httpProviders.clear();
            this.httpProviders.addAll(Arrays.asList(providers));
            return this;
        }

        public Builder addHttpProvider(HttpProvider providers) {
            this.httpProviders.add(providers);
            return this;
        }


        public ProviderManager build() {
            if (httpProviders == null || httpProviders.size() == 0) {
                // TODO(tkk) replace with more specific type of exception
                throw new IllegalStateException("can't initialize a ProviderManager instance with empty HttpProviders");
            }
            return new ProviderManager(this);
        }

    }
}

//用户需要