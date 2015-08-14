package com.sungardas.snapdirector.rest.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;

public class JsonFromStream {
	public static final Log LOG = LogFactory.getLog(JsonFromStream.class);
	private static final String JSON_OBJ_EXCEPTION_MESSAGE = "Can't parse JSON as a JSONObject.";
	private static final String JSON_ARR_EXCEPTION_MESSAGE = "Can't parse JSON as a JSONOArray.";

	
	public static JSONObject newJSONObject(InputStream is) {
		JSONObject result = null;
		try  {
			JSONTokener tokener = new JSONTokener(is);
			result = new JSONObject(tokener);
		} catch (JSONException cantParseJSON) {
			LOG.error(JSON_OBJ_EXCEPTION_MESSAGE, cantParseJSON);
			throw cantParseJSON; 
		}

		return result;
	}


	public static JSONArray newJSONArray(InputStream is) {
		JSONArray result = null;
		try  {
			JSONTokener tokener = new JSONTokener(is);
			result = new JSONArray(tokener);
		} catch (JSONException cantParseJSON) {
			LOG.error(JSON_ARR_EXCEPTION_MESSAGE, cantParseJSON);
			throw cantParseJSON;
		}
		return result;
	}
}
