package com.fyp.faaiz.ets.util;

import org.json.JSONObject;

public class Helpers {

    public static boolean contains(JSONObject jsonObject, String key) {
        return jsonObject != null && jsonObject.has(key) && !jsonObject.isNull(key) ? true : false;
    }

}
