package germany.jannismartensen.smartmanaging.Endpoints;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EndpointUtil {
    public static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

    public  static Map<String, String> streamToMap(InputStream is) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;


        while ((line = br.readLine()) != null) {
            content.append(line);
            content.append("\n");
        }

        String[] values = content.toString().split("&");
        Map<String, String> map = new HashMap<>();
        for (String s : values) {
            String[] sl = s.split("=");
            map.put(sl[0], sl[1]);
        }

        return map;
    }

}
