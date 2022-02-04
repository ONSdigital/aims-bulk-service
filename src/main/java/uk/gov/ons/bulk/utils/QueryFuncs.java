package uk.gov.ons.bulk.utils;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;

public class QueryFuncs {

    private static QueryFuncs _instance = new QueryFuncs();

    private QueryFuncs() {


    }

    public static QueryFuncs getInstance() {

        return _instance;
    }


    public static Iterable<FieldValueList> runQuery(String queryText,BigQuery bigquery, Boolean isTest) throws  java.lang.InterruptedException, ClassNotFoundException, IOException, NoSuchAlgorithmException {

        if (queryText == "") return new ArrayList<FieldValueList>();

        if (isTest) {
            System.out.println("TESTING!");
            return getResponse(queryText);

        }

         QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryText).build();

        return bigquery.query(queryConfig).iterateAll();

    }

    public static ArrayList<FieldValueList> getResponse(String query) throws ClassNotFoundException, IOException, NoSuchAlgorithmException {

        Properties queryReponse = new Properties();

        String queryReponseRef = "query.properties";

        String key = convertToMd5(query);

        InputStream is = QueryFuncs.class.getClassLoader().getResourceAsStream(queryReponseRef);
        if (is != null) {
            queryReponse.load(is);
        } else {
            throw new FileNotFoundException("Query Property file not in classpath");
        }
        String result = (String) queryReponse.get(key);

        System.out.println(result);

        ArrayList<FieldValueList> fields = (ArrayList<FieldValueList>) deserializeFromBase64(result);

        if(query != null)
            return fields;
        return null;

    }

    public static String convertToMd5(String text ) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(text.getBytes(Charset.forName("UTF8")));

        byte[] resultByte = md.digest();

        return new String(Hex.encodeHex(resultByte));

    }

    public static Object deserializeFromBase64(String s) throws IOException,ClassNotFoundException {

        byte [] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data) );
        Object o = ois.readObject();
        ois.close();
        return o;
    }
}
