package uk.gov.ons.bulk.utils;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;

public class QueryFuncs {


    public Iterable<FieldValueList> runQuery(String queryText,BigQuery bigquery) throws  java.lang.InterruptedException {
        if (queryText == "") return new ArrayList<FieldValueList>();
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryText).build();

        return bigquery.query(queryConfig).iterateAll();

    }
}
