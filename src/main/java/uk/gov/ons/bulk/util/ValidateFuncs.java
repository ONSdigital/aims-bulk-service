package uk.gov.ons.bulk.util;

import com.google.api.client.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkRequestParamsErrors;

@Slf4j
public class ValidateFuncs {

    public static BulkRequestParamsErrors validateBulkParams(
                                    String limitperaddress,
                                    String classificationfilter,
                                    String historical,
                                    String matchthreshold,
                                    String verbose,
                                    String epoch,
                                    String excludeengland,
                                    String excludenorthernireland,
                                    String excludescotland,
                                    String excludewales
    ) {

        BulkRequestParamsErrors paramErrors = new BulkRequestParamsErrors();

        // This is more work than using built in validation, but allows for more advanced validation as per the Scala API

        paramErrors.setClassificationfilter(validateClassificationFilter(classificationfilter));
        paramErrors.setEboost(validateEboost(excludeengland));
        paramErrors.setEpoch(validateEpoch(epoch));
        paramErrors.setHistorical(validateHistorical(historical));
        paramErrors.setLimit(validateLimit(limitperaddress));
        paramErrors.setMatchthreshold(validateMatchthreshold(matchthreshold));
        paramErrors.setSboost(validateSboost(excludescotland));
        paramErrors.setVerbose(validateVerbose(verbose));
        paramErrors.setSboost(validateWboost(excludewales));
        paramErrors.setSboost(validateNboost(excludenorthernireland));

        paramErrors.setMessage(paramErrors.getClassificationfilter()
                + paramErrors.getEboost()
                + paramErrors.getEpoch()
                + paramErrors.getHistorical()
                + paramErrors.getLimit()
                + paramErrors.getMatchthreshold()
                + paramErrors.getVerbose()
                + paramErrors.getSboost()
                + paramErrors.getNboost()
                + paramErrors.getWboost());

        return paramErrors;
    }

    public static String validateClassificationFilter(String paramVal){

         if (paramVal.contains("*") && paramVal.contains(","))
             return "classification filter may not contain a list and a wildcard; ";
         else
             return "";

    }

    public static String validateEboost(String paramVal){

        if (paramVal.equalsIgnoreCase("false") || paramVal.equalsIgnoreCase("true"))
                return "";
        else
            return "excludeengland must be true or false; ";
    }

    public static String validateEpoch(String paramVal){

        if (paramVal.equals("89") || paramVal.equals("87") || paramVal.equals("80") || paramVal.equals("39") )
            return "";
        else
            return "epoch must be one of 89,87,80,39; ";

    }

    public static String validateHistorical(String paramVal){
        if (paramVal.equalsIgnoreCase("false") || paramVal.equalsIgnoreCase("true"))
            return "";
        else
            return "historical must be true or false; ";
    }


    public static String validateNboost(String paramVal){
        if (paramVal.equalsIgnoreCase("false") || paramVal.equalsIgnoreCase("true"))
            return "";
        else
            return "excludenorthernireland must be true or false; ";
    }

    public static String validateLimit(String paramVal){

        if (isNumber(paramVal) && Integer.parseInt(paramVal) > 0 && Integer.parseInt(paramVal) < 101 )
            return "";
        else
            return "limitperaddress must be an integer between 1 and 100; ";


    }

    public static String validateMatchthreshold(String paramVal){
        if (isNumber(paramVal) && Integer.parseInt(paramVal) > 0 && Integer.parseInt(paramVal) < 101 )
            return "";
        else
            return "matchthreshold must be an integer between 1 and 100; ";
    }

    public static String validateWboost(String paramVal){
        if (paramVal.equalsIgnoreCase("false") || paramVal.equalsIgnoreCase("true"))
            return "";
        else
            return "excludewales must be true or false; ";
    }

    public static String validateSboost(String paramVal){
        if (paramVal.equalsIgnoreCase("false") || paramVal.equalsIgnoreCase("true"))
            return "";
        else
            return "excludescotland must be true or false; ";
    }

    public static String validateVerbose(String paramVal){
        if (paramVal.equalsIgnoreCase("false") || paramVal.equalsIgnoreCase("true"))
            return "";
        else
            return "verbose must be true or false; ";
    }

    public static boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

}
