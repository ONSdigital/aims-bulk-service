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

    public static Iterable<FieldValueList> runQuery(String queryText,BigQuery bigquery) throws  java.lang.InterruptedException, ClassNotFoundException, IOException, NoSuchAlgorithmException {

        if (queryText == "") return new ArrayList<FieldValueList>();
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryText).build();
        return bigquery.query(queryConfig).iterateAll();

    }

}

