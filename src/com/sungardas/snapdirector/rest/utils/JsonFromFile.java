package com.sungardas.snapdirector.rest.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;


public class JsonFromFile {

	public static final Log LOG = LogFactory.getLog(JsonFromFile.class);

	private static final String JSON_OBJ_EXCEPTION_MESSAGE = "Can't parse JSON as a JSONObject. See source file %s";
	private static final String JSON_ARR_EXCEPTION_MESSAGE = "Can't parse JSON as a JSONOArray. See source file %s";
	private static final String IO_EXCEPTION_MESSAGE = "Can't read from source file %s";


	public static JSONObject newJSONObject(String filename) {
		JSONObject result = null;
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
			JSONTokener tokener = new JSONTokener(is);
			result = new JSONObject(tokener);
		} catch (JSONException cantParseJSON) {
			LOG.error(format(JSON_OBJ_EXCEPTION_MESSAGE, filename), cantParseJSON);
			throw cantParseJSON; 
		} catch (IOException ioException) {
			LOG.error(format(IO_EXCEPTION_MESSAGE,filename), ioException);
			throw new RuntimeException(ioException);
		}

		return result;
	}


	public static JSONArray newJSONArray(String filename) {
		JSONArray result = null;
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
			JSONTokener tokener = new JSONTokener(is);
			result = new JSONArray(tokener);
		} catch (JSONException cantParseJSON) {
			LOG.error(format(JSON_ARR_EXCEPTION_MESSAGE, filename), cantParseJSON);
			throw cantParseJSON;
		} catch (IOException ioException) {
			LOG.error(format(IO_EXCEPTION_MESSAGE,filename), ioException);
			throw new RuntimeException(ioException);
		}
		return result;
	}
}
