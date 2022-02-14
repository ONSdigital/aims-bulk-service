package uk.gov.ons.bulk.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.google.cloud.bigquery.*;
import org.apache.commons.codec.binary.Hex;

public class QueryFuncs {

    private static QueryFuncs _instance = new QueryFuncs();

    private QueryFuncs() {


    }

    public static QueryFuncs getInstance() {

        return _instance;
    }


    /**
     * The method runBqQuery is a generic method to call any BigQuery Sql. It will return the result
     * of BigQuery Sql as a collection of FieldValueList objects obtained from the TableResult.
     *
     * @param query : sql query to execute in bigquery
     * @return
     * @throws InterruptedException
     */
    public ArrayList<FieldValueList> runQuery(String query, BigQuery bigquery) throws InterruptedException {
        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build();

        JobId jobId = JobId.of(UUID.randomUUID().toString());

        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        TableResult result = queryJob.getQueryResults();

        Iterator iter = result.getValues().iterator();

        ArrayList<FieldValueList> fieldResults = new ArrayList<FieldValueList>();
        while (iter.hasNext())
        {
            fieldResults.add((FieldValueList) iter.next());
        }

        return fieldResults;
    }



    public Object deserializeFromBase64( String s ) throws IOException ,ClassNotFoundException {

        byte [] data = Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(  data ) );

        Object o  = ois.readObject();
        ois.close();
        return o;
    }

    public String serializeToBase64(Serializable o) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }


    public Properties getPropertyFile() throws IOException {

        Properties prop = new Properties();
        InputStream in = getClass().getClassLoader().getResourceAsStream("query.properties");

        prop.load(in);

        return prop;

    }

    public String convertToMd5(String text ) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(text.getBytes(Charset.forName("UTF8")));

        byte[] resultByte = md.digest();

        return new String(Hex.encodeHex(resultByte));

    }


}

