package seniorgps.config;

public class DataSettings {
	private static String	textPersonName;
	private static String	textPersonAddress;
	private static String	textPhoneNumber;
	private static int	gpsRange;
	private static String	userEmail;
	private static String	userPassword;
	private static int	timeSMSAlertInSeconds;
	private static String	textAlertSMS;
	public static boolean	isRunning;
    public static boolean   monitoring;
    public static boolean   sendSMS;
	static {
		userEmail = "senior@gps.cz";
		userPassword = "hello";
		textPersonName = "Your Name";
		textPersonAddress = "Your Address";
		textPhoneNumber = "+123 987 654 321";
		textAlertSMS = "";
        monitoring = false;
        sendSMS = false;
		gpsRange = 20; // collision range
		timeSMSAlertInSeconds = 30; // 3 = 6s; 30 = 1min; 300 = 10min;
		isRunning = false;
	}

	public static String getTextAlertSMS() {
		return textAlertSMS;
	}

	public static void setTextAlertSMS(String textAlertSMS) {
		DataSettings.textAlertSMS = textAlertSMS;
	}

	public static int getTimeSMSAlertInSeconds() {
		return timeSMSAlertInSeconds;
	}

	public static String getUserEmail() {
		return userEmail;
	}
	
	public static void setUserEmail(String email) {
		DataSettings.userEmail = email;
	}
	
	public static void setUserPassword(String password) {
		DataSettings.userPassword = password;
	}

	public static String getUserPassword() {
		return userPassword;
	}

	public static String getUserCredentials() {		
		return userEmail + ":" + userPassword;
	}

	public static String getTextPersonName() {
		return textPersonName;
	}

	public static void setTextPersonName(String textPersonName) {
		DataSettings.textPersonName = textPersonName;
	}

	public static String getTextPhoneNumber() {
		return textPhoneNumber;
	}

	public static void setTextPhoneNumber(String textPhoneNumber) {
		DataSettings.textPhoneNumber = textPhoneNumber;
	}


	public static int getGpsRange() {
		return gpsRange;
	}

	public static void setGpsRange(int gpsRange) {
		DataSettings.gpsRange = gpsRange;
	}

	public static String getTextPersonAddress() {
		return textPersonAddress;
	}

	public static void setTextPersonAddress(String textPersonAddress) {
		DataSettings.textPersonAddress = textPersonAddress;
	}

	public static String getAllDataAsString() {
		String string = "";

		string += "person_name:" + textPersonName + "\n";
		string += "person_address:" + textPersonAddress + "\n";
		string += "phone_number:" + textPhoneNumber + "\n";
		string += "gps_range:" + gpsRange + "\n";
        string += "smsCheck:" + sendSMS + "\n";
        string += "monitoringSwitch:" + monitoring + "\n";

		return string;
	}
	
	public static String getLoginDataAsString() {
		String string = "";
		
		string += "email " + userEmail+ ":";
		string += "pass " +  userPassword + "\n";
		
		return string;
	}

    public static boolean isSendSMS() {
        return sendSMS;
    }

    public static void setSendSMS(boolean sendSMS) {
        DataSettings.sendSMS = sendSMS;
    }

    public static boolean isMonitoring() {
        return monitoring;
    }

    public static void setMonitoring(boolean monitoring) {
        DataSettings.monitoring = monitoring;
    }
}
