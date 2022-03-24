package uk.gov.ons.bulk.util;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkRequestParams;
import uk.gov.ons.bulk.entities.BulkRequestParamsErrors;

@Slf4j
public class ValidateFuncs {

    public static BulkRequestParamsErrors validateBulkParams(BulkRequestParams bps) {

        BulkRequestParamsErrors paramErrors = new BulkRequestParamsErrors();

        // This is more work than using built in validation, but allows for more advanced validation as per the Scala API

        paramErrors.setClassificationfilter(validateClassificationFilter(bps.getClassificationfilter()));
        paramErrors.setEboost(validateEboost(bps.getEboost()));
        paramErrors.setEpoch(validateEpoch(bps.getEpoch()));
        paramErrors.setHistorical(validateHistorical(bps.getHistorical()));
        paramErrors.setIncludeauxiliarysearch(validateIncludeauxiliarysearch(bps.getIncludeauxiliarysearch()));
        paramErrors.setLat(validateLat(bps.getLat()));
        paramErrors.setLon(validateLon(bps.getLon()));
        paramErrors.setLimit(validateLimit(bps.getLimit()));
        paramErrors.setMatchthreshold(validateMatchthreshold(bps.getMatchthreshold()));
        paramErrors.setOffset(validateOffset(bps.getOffset()));
        paramErrors.setRangekm(validateRangekm(bps.getRangekm()));
        paramErrors.setSboost(validateSboost(bps.getSboost()));
        paramErrors.setVerbose(validateVerbose(bps.getVerbose()));

        return paramErrors;
    }

    public static String validateClassificationFilter(String paramVal){
        return "OK";
    }

    public static String validateEboost(String paramVal){
        return "OK";
    }

    public static String validateEpoch(String paramVal){
        return "OK";
    }

    public static String validateHistorical(String paramVal){
        return "OK";
    }

    public static String validateIncludeauxiliarysearch(String paramVal){
        return "OK";
    }

    public static String validateLat(String paramVal){
        return "OK";
    }

    public static String validateLon(String paramVal){
        return "OK";
    }

    public static String validateLimit(String paramVal){
        return "OK";
    }

    public static String validateMatchthreshold(String paramVal){
        return "OK";
    }

    public static String validateOffset(String paramVal){
        return "OK";
    }

    public static String validateRangekm(String paramVal){
        return "OK";
    }

    public static String validateSboost(String paramVal){
        return "OK";
    }

    public static String validateVerbose(String paramVal){
        return "OK";
    }

}
