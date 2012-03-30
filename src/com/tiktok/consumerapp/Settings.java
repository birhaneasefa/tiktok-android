//-----------------------------------------------------------------------------
// Settings
//-----------------------------------------------------------------------------

package com.tiktok.consumerapp;

//-----------------------------------------------------------------------------
// imports
//-----------------------------------------------------------------------------

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

//-----------------------------------------------------------------------------
// class implementation
//-----------------------------------------------------------------------------

public final class Settings implements OnSharedPreferenceChangeListener
{

    /**
     * [moiz] use apply instead of commit where possible, this runs the save
     *   process asychronously instead of commit which runs it synchronously,
     *   can't use for remove
     */

    //-------------------------------------------------------------------------
    // statics
    //-------------------------------------------------------------------------

    public static final String kDomain           = "com.tiktok.comsumerapp.preferences";
    public static final String kTagName          = "TTS_name";
    public static final String kTagEmail         = "TTS_email";
    public static final String kTagGender        = "TTS_gender";
    public static final String kTagBirthday      = "TTS_birthday";
    public static final String kTagHome          = "TTS_home";
    public static final String kTagHomeLocality  = "TTS_homeLocality";
    public static final String kTagWork          = "TTS_work";
    public static final String kTagWorkLocality  = "TTS_workLocality";
    public static final String kTagLastUpdate    = "TTS_lastUpdate";
    //public static final String kTagTutorialIndex = "TTS_tutorial";

    //-------------------------------------------------------------------------
    // constructor
    //-------------------------------------------------------------------------

    public Settings(Context context)
    {
        mContext     = context.getApplicationContext();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEditor      = mPreferences.edit();

        mPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    //-------------------------------------------------------------------------
    // settings
    //-------------------------------------------------------------------------

    public String name()
    {
        return mPreferences.getString(kTagName, "");
    }

    //-------------------------------------------------------------------------

    public void setName(String name)
    {
        mEditor.putString(kTagName, name);
        mEditor.commit();
    }

    //-------------------------------------------------------------------------

    public void syncName()
    {
        TikTokApi api = new TikTokApi(mContext);
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("name", name());
        api.updateSettings(settings);
    }

    //-------------------------------------------------------------------------

    public String email()
    {
        return mPreferences.getString(kTagEmail, "");
    }

    //-------------------------------------------------------------------------

    public void setEmail(String email)
    {
        mEditor.putString(kTagEmail, email);
        mEditor.commit();
    }

    //-------------------------------------------------------------------------

    public void syncEmail()
    {
        TikTokApi api = new TikTokApi(mContext);
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("email", email());
        api.updateSettings(settings);
    }

    //-------------------------------------------------------------------------

    public String gender()
    {
        return mPreferences.getString(kTagGender, "");
    }

    //-------------------------------------------------------------------------

    public void setGender(String gender)
    {
        mEditor.putString(kTagGender, gender);
        mEditor.commit();
    }

    //-------------------------------------------------------------------------

    public void syncGender()
    {
        // push update to server
        TikTokApi api = new TikTokApi(mContext);
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("sex", gender().toLowerCase().substring(0, 1));
        api.updateSettings(settings);

        // update analytics
        Analytics.setUserGender(gender());
    }

    //-------------------------------------------------------------------------

    public Date birthday()
    {
        String birthday = mPreferences.getString(kTagBirthday, null);
        try {
            return birthday == null ? null :
                new SimpleDateFormat("yyyy.MM.dd", Locale.US).parse(birthday);
        } catch (ParseException e) {
            return null;
        }
    }

    //-------------------------------------------------------------------------

    public String birthdayStr()
    {
        // format date into string
        Date birthday               = birthday();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, y", Locale.US);
        return birthday == null ? "" : dateFormat.format(birthday());
    }

    //-------------------------------------------------------------------------

    public void setBirthday(Date birthday)
    {
        mEditor.putString(kTagBirthday, DateFormat.getDateInstance().format(birthday));
        mEditor.commit();
    }

    //-------------------------------------------------------------------------

    public void syncBirthday()
    {
        // push update to server
        TikTokApi api = new TikTokApi(mContext);
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("birthday", birthdayStr());
        api.updateSettings(settings);

        // update analytics
        Analytics.setUserAge(birthday());
    }

    //-------------------------------------------------------------------------

    public Location home()
    {
        return getLocation(kTagHome);
    }

    //-------------------------------------------------------------------------

    public void setHome(Location home)
    {
        setLocation(kTagHome, home);
    }

    //-------------------------------------------------------------------------

    public void syncHome()
    {
        TikTokApi api = new TikTokApi(mContext);
        api.updateHomeLocation(home());
    }

    //-------------------------------------------------------------------------

    public String homeLocality()
    {
        return mPreferences.getString(kTagHomeLocality, "");
    }

    //-------------------------------------------------------------------------

    public void setHomeLocality(String homeLocality)
    {
         mEditor.putString(kTagHomeLocality, homeLocality);
         mEditor.commit();
    }

    //-------------------------------------------------------------------------

    public Location work()
    {
        return getLocation(kTagWork);
    }

    //-------------------------------------------------------------------------

    public void setWork(Location work)
    {
        setLocation(kTagWork, work);
    }

    //-------------------------------------------------------------------------

    public void syncWork()
    {
        TikTokApi api = new TikTokApi(mContext);
        api.updateWorkLocation(work());
    }

    //-------------------------------------------------------------------------

    public String workLocality()
    {
        return mPreferences.getString(kTagWorkLocality, "");
    }

    //-------------------------------------------------------------------------

    public void setWorkLocality(String workLocality)
    {
         mEditor.putString(kTagWorkLocality, workLocality);
         mEditor.commit();
    }

    //-------------------------------------------------------------------------

    public Date lastUpdate()
    {
        long lastUpdate = mPreferences.getLong(kTagLastUpdate, 0);
        return new Date(lastUpdate);
    }

    //-------------------------------------------------------------------------

    public void setLastUpdate(Date lastUpdate)
    {
         mEditor.putLong(kTagLastUpdate, lastUpdate.getTime());
         mEditor.commit();
    }

    //-------------------------------------------------------------------------
    // methods
    //-------------------------------------------------------------------------

    public void clearAllSettings()
    {
        mEditor.clear();
        mEditor.commit();
    }

    //-------------------------------------------------------------------------

    private Location getLocation(String key)
    {
        // generate keys for parts
        String latKey = key + "_lat";
        String lngKey = key + "_lng";

        // grab coordinates from settings
        float latitude  = mPreferences.getFloat(latKey, 0.0f);
        float longitude = mPreferences.getFloat(lngKey, 0.0f);

        // encapsulate data in location object
        String provider   = null;
        Location location = new Location(provider);
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return location;
    }

    //-------------------------------------------------------------------------

    private void setLocation(String key, Location value)
    {
        // generate keys for parts
        String latKey = key + "_lat";
        String lngKey = key + "_lng";

        // update preferences
        mEditor.putFloat(latKey, (float)value.getLatitude());
        mEditor.putFloat(lngKey, (float)value.getLongitude());
        mEditor.commit();
    }

    //-------------------------------------------------------------------------
    // shared preferences listener
    //-------------------------------------------------------------------------

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
        Log.i("Settings", String.format("Syncing setting %s", key));

        if (key.equals(kTagName)) {
            syncName();
        } else if (key.equals(kTagEmail)) {
            syncEmail();
        } else if (key.equals(kTagGender)) {
            syncGender();
        } else if (key.equals(kTagBirthday)) {
            syncBirthday();
        }
    }

    //-------------------------------------------------------------------------
    // fields
    //-------------------------------------------------------------------------

    private Context           mContext;
    private SharedPreferences mPreferences;
    private Editor            mEditor;

}