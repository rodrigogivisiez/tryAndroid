package com.tullyapp.tully.Utils;

/**
 * Created by macbookpro on 12/09/17.
 */

public class APIs {

    public static final String SYNONYM_WORD = "https://api.datamuse.com/words";

    private static final String STRIPE_TEST_KEY = "pk_test_uzn7QmKdx4dVieKmhbK8Psm1";
    private static final String STRIPE_PRODUCTION_KEY = "pk_live_RbkW4UJxFAVAs164pxM6lCFQ";

    public static final String STRIPE_KEY = STRIPE_TEST_KEY;

    private static final String TEST_SERVER = "http://34.227.113.99/";
    private static final String PRODUCTION_SERVER = "https://tullyconnect.com/";

    private static final String BASE_URL = TEST_SERVER;

    public static final String SHARE_PROJECT = BASE_URL+"api/share/project";
    public static final String SHARE_AUDIO = BASE_URL+"api/share/audiofile";
    public static final String SHARE_MASTER = BASE_URL+"api/share/master";
    public static final String SHARE_BEAT = BASE_URL+"api/share/beat";
    public static final String SHARE_RECORDING = BASE_URL+"api/share/recordings";
    public static final String INVITE_ENGINEER = BASE_URL+"api/engineer/invite";
    public static final String GET_BEATS_LIST = BASE_URL+"mobile/api/instore/get";
    public static final String CHARGE_URL = BASE_URL+"mobile/api/payment/marketplace_beat";
    public static final String SUBSCRIBE_AUDIO_ANALYZER = BASE_URL+"mobile/api/payment/subscribe_audio_analyzer";
    public static final String SUBSCRIBE_ENGINEER_ADMIN_ACCESS = BASE_URL+"mobile/api/payment/subscribe_engineer_admin_access";
    public static final String CANCEL_AUDIO_ANALYZER_SUBSCRIPTION = BASE_URL+"mobile/api/payment/cancel_audio_analyzer_subscription";
    public static final String SELL_FREE_BEAT = BASE_URL+"mobile/api/payment/sellFreeBeats";
    public static final String IMPORT_AUDIO_URL = BASE_URL+"artist/dashboard";

    public static final String SUBSCRIBE_COLLABORATOR = BASE_URL + "tully_qa/mobile/api/payment/subscribe_invite_collaboration";
    public static final String CHECK_EMAIL_EXISTANCE = BASE_URL + "tully_qa/mobile/api/collaboration/check_email_exist";

    public static final String SHARE_LYRICS = BASE_URL+"api/share/lyrics";
}