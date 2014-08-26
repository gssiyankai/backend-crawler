package lib;

import net.sf.json.JSONObject

/**
 * Writes out the JSON data file.
 */
public class DataWriter {
    public static void write(String key,JSONObject envelope) {
        println envelope.toString(2)

        // write unsigned data to *.json because of JENKINS-15105
        File d = new File("target")
        d.mkdirs()
        new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})","UTF-8");
    }
}
