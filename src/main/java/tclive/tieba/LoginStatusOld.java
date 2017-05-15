package tclive.tieba;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tclive.TCLiveMod;
import tclive.util.Utils;

@Deprecated
public class LoginStatusOld {
	
	protected boolean hasLogined;
	protected int lastErrorCode = 0;
	
	private static String static_tokenUrl = "https://passport.baidu.com/v2/api/?getapi&tpl=tb&apiver=v3&tt=%d&class=login&gid=%s&logintype=dialogLogin&callback=%s";
	private static String static_rsaUrl = "https://passport.baidu.com/v2/getpublickey?token=%s&tpl=tb&apiver=v3&tt=%d&gid=%s&callback=%s";
	private static String static_loginUrl = "https://passport.baidu.com/v2/api/?login";
	private static String static_codeUrl = "https://passport.baidu.com/cgi-bin/genimage?";
	private static String static_verifyUrl = "https://passport.baidu.com/v2/?checkvcode&token=%s&tpl=tb&apiver=v3&tt=%d&verifycode=%s&codestring=%s&callback=%s";
	private static String static_checkUrl = "https://passport.baidu.com/v2/api/?logincheck&token=%s&tpl=tb&apiver=v3&tt=%d&sub_source=leadsetpwd&username=%s&isphone=false&callback=%s";
	
	private static String param_callback_token = Utils.genCallback(), param_callback_rsa = Utils.genCallback(), param_callback_login = Utils.genCallback();
	private static String param_gid = Utils.genGid();
	private static String param_token, param_codeString;
	private static String param_rsaKey, param_publicKey;
	
	private static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";
	
	protected CloseableHttpClient httpClient;
	protected final JsonParser jsonParser;
	
	public LoginStatusOld() {
		resetStatus();
		jsonParser = TCLiveMod.mod.jsonParser;
	}
	
	public void resetStatus() {
		hasLogined = false;
		try {
			if (httpClient != null)
				httpClient.close();
		} catch (IOException e) {
		}
		TCLiveMod.mod.cookieStore.clear();
		TCLiveMod.mod.httpClient = HttpClients.custom().setDefaultCookieStore(TCLiveMod.mod.cookieStore).build();
		httpClient = TCLiveMod.mod.httpClient;
	}
	
	public boolean hasLogined() {
		return hasLogined;
	}
	
	public int getLastError() {
		return lastErrorCode;
	}
	
	protected boolean checkCanLogin() {
		return !TCLiveMod.mod.tb_username.isEmpty() && !TCLiveMod.mod.tb_password.isEmpty();
	}
	
	private String checkVerifyCode(String token) {
		try {
			HttpGet httpPost = new HttpGet(String.format(static_checkUrl, token, System.currentTimeMillis(), TCLiveMod.mod.tb_username, Utils.genCallback()));
			CloseableHttpResponse response = httpClient.execute(httpPost);
			String json = EntityUtils.toString(response.getEntity());
			json = json.substring(json.indexOf("(") + 1, json.lastIndexOf(")"));
			JsonObject checkJson = jsonParser.parse(json).getAsJsonObject();
			response.close();
			return checkJson.getAsJsonObject("data").get("codeString").getAsString();
		} catch (Exception e) {
			return "";
		}
	}
	
	public void tryLogin() {
		try {
			if (!checkCanLogin()) {
				lastErrorCode = -2;
				return;
			}
			httpClient.execute(new HttpGet("https://tieba.baidu.com/")).close();
			String tokenUrl = String.format(static_tokenUrl, System.currentTimeMillis(), param_gid, param_callback_token);
			CloseableHttpResponse response = httpClient.execute(new HttpGet(tokenUrl));
			String tokenJson = EntityUtils.toString(response.getEntity());
			response.close();
			tokenJson = tokenJson.substring(tokenJson.indexOf("(") + 1, tokenJson.lastIndexOf(")"));
			JsonObject jsonObject = jsonParser.parse(tokenJson).getAsJsonObject().getAsJsonObject("data");
			param_token = jsonObject.get("token").getAsString();
			param_codeString = checkVerifyCode(param_token);
			if (!param_codeString.isEmpty()) {
				lastErrorCode = 257;
				return;
			}
			String rsaUrl = String.format(static_rsaUrl, param_token, System.currentTimeMillis(), param_gid, param_callback_rsa);
			response = httpClient.execute(new HttpGet(rsaUrl));
			String rsaJson = EntityUtils.toString(response.getEntity());
			response.close();
			rsaJson = rsaJson.substring(rsaJson.indexOf("(") + 1, rsaJson.lastIndexOf(")"));
			jsonObject = jsonParser.parse(rsaJson).getAsJsonObject();
			param_rsaKey = jsonObject.get("key").getAsString();
			param_publicKey = jsonObject.get("pubkey").getAsString();
			param_publicKey = param_publicKey.substring(param_publicKey.indexOf("\n") + 1, param_publicKey.indexOf("-----END PUBLIC KEY-----") - 1);
			PublicKey publicKey = Utils.getPublicKey(param_publicKey);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			String param_password = new String(Base64.encodeBase64(cipher.doFinal(TCLiveMod.mod.tb_password.getBytes())));
			HttpPost httpPost = new HttpPost(static_loginUrl);
			List<BasicNameValuePair> paramList = Lists.newArrayList();
			paramList.add(new BasicNameValuePair("staticpage", "https://tieba.baidu.com/tb/static-common/html/pass/v3Jump.html"));
			paramList.add(new BasicNameValuePair("charset", "utf-8"));
			paramList.add(new BasicNameValuePair("token", param_token));
			paramList.add(new BasicNameValuePair("tpl", "tb"));
			paramList.add(new BasicNameValuePair("subpro", ""));
			paramList.add(new BasicNameValuePair("apiver", "v3"));
			paramList.add(new BasicNameValuePair("tt", String.valueOf(System.currentTimeMillis())));
			paramList.add(new BasicNameValuePair("codestring", param_codeString));
			paramList.add(new BasicNameValuePair("safeflg", "0"));
			paramList.add(new BasicNameValuePair("u", "https://tieba.baidu.com/index.html#"));
			paramList.add(new BasicNameValuePair("isPhone", ""));
			paramList.add(new BasicNameValuePair("detect", "1"));
			paramList.add(new BasicNameValuePair("gid", param_gid));
			paramList.add(new BasicNameValuePair("quick_user", "0"));
			paramList.add(new BasicNameValuePair("logintype", "dialogLogin"));
			paramList.add(new BasicNameValuePair("logLoginType", "pc_loginDialog"));
			paramList.add(new BasicNameValuePair("idc", ""));
			paramList.add(new BasicNameValuePair("loginmerge", "true"));
			paramList.add(new BasicNameValuePair("splogin", "rate"));
			paramList.add(new BasicNameValuePair("username", TCLiveMod.mod.tb_username));
			paramList.add(new BasicNameValuePair("password", param_password));
			paramList.add(new BasicNameValuePair("mem_pass", "on"));
			paramList.add(new BasicNameValuePair("rsakey", param_rsaKey));
			paramList.add(new BasicNameValuePair("crypttype", "12"));
			paramList.add(new BasicNameValuePair("ppui_logintime", "6523"));
			paramList.add(new BasicNameValuePair("countrycode", ""));
			paramList.add(new BasicNameValuePair("fp_uid", ""));
			paramList.add(new BasicNameValuePair("callback", "parent." + param_callback_login));
			httpPost.setEntity(new UrlEncodedFormEntity(paramList, "utf-8"));
			response = httpClient.execute(httpPost);
			String responseString = EntityUtils.toString(response.getEntity());
			param_codeString = Utils.patternMatch(responseString, "&codeString=(\\w+)&");
			lastErrorCode = Integer.parseInt(Utils.patternMatch(responseString, "err_no=(\\d+)"));
			response.close();
			checkSuccess();
			if (lastErrorCode == 0) hasLogined = true;
		} catch (Exception e) {
			e.printStackTrace();
			lastErrorCode = -1;
		}
	}
	
	protected void checkSuccess() {
		if (TCLiveMod.mod.cookieStore.getCookies().stream().anyMatch(cookie -> cookie.getName().equals("BDUSS"))) {
			lastErrorCode = 0;
			hasLogined = true;
		}
	}
	
	public void loadVerifyCodePicture() {
		try {
			String codeUrl = static_codeUrl + param_codeString;
			Utils.downLoadFromUrl(codeUrl, "veri_code.png", "./");
		} catch (Exception e) {
			TCLiveMod.LOG.warn("Unable to download verify code picture.");
		}
	}
	
	public void tryLoginWithCode(String veri_code) {
		try {
			if (!checkCanLogin()) {
				lastErrorCode = -1;
				return;
			}
//			httpClient.execute(new HttpGet("http://tieba.baidu.com/")).close();
//			String tokenUrl = String.format(static_tokenUrl, System.currentTimeMillis(), param_gid, param_callback_token);
//			CloseableHttpResponse response = httpClient.execute(new HttpGet(tokenUrl));
//			String tokenJson = EntityUtils.toString(response.getEntity());
//			response.close();
//			tokenJson = tokenJson.substring(tokenJson.indexOf("(") + 1, tokenJson.lastIndexOf(")"));
//			JsonObject jsonObject = jsonParser.parse(tokenJson).getAsJsonObject().getAsJsonObject("data");
//			param_token = jsonObject.get("token").getAsString();
			String rsaUrl = String.format(static_rsaUrl, param_token, System.currentTimeMillis(), param_gid, param_callback_rsa);
			CloseableHttpResponse response = httpClient.execute(new HttpGet(rsaUrl));
			String rsaJson = EntityUtils.toString(response.getEntity());
			response.close();
			rsaJson = rsaJson.substring(rsaJson.indexOf("(") + 1, rsaJson.lastIndexOf(")"));
			JsonObject jsonObject = jsonParser.parse(rsaJson).getAsJsonObject();
			param_rsaKey = jsonObject.get("key").getAsString();
			param_publicKey = jsonObject.get("pubkey").getAsString();
			param_publicKey = param_publicKey.substring(param_publicKey.indexOf("\n") + 1, param_publicKey.indexOf("-----END PUBLIC KEY-----") - 1);
			PublicKey publicKey = Utils.getPublicKey(param_publicKey);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			String param_password = new String(Base64.encodeBase64(cipher.doFinal(TCLiveMod.mod.tb_password.getBytes())));
			List<BasicNameValuePair> paramList = Lists.newArrayList();
			paramList.add(new BasicNameValuePair("staticpage", "https://tieba.baidu.com/tb/static-common/html/pass/v3Jump.html"));
			paramList.add(new BasicNameValuePair("charset", "utf-8"));
			paramList.add(new BasicNameValuePair("token", param_token));
			paramList.add(new BasicNameValuePair("tpl", "tb"));
			paramList.add(new BasicNameValuePair("subpro", ""));
			paramList.add(new BasicNameValuePair("apiver", "v3"));
			paramList.add(new BasicNameValuePair("tt", String.valueOf(System.currentTimeMillis())));
			paramList.add(new BasicNameValuePair("codestring", param_codeString));
			paramList.add(new BasicNameValuePair("safeflg", "0"));
			paramList.add(new BasicNameValuePair("u", "https://tieba.baidu.com/index.html#"));
			paramList.add(new BasicNameValuePair("isPhone", ""));
			paramList.add(new BasicNameValuePair("detect", "1"));
			paramList.add(new BasicNameValuePair("gid", param_gid));
			paramList.add(new BasicNameValuePair("quick_user", "0"));
			paramList.add(new BasicNameValuePair("logintype", "dialogLogin"));
			paramList.add(new BasicNameValuePair("logLoginType", "pc_loginDialog"));
			paramList.add(new BasicNameValuePair("idc", ""));
			paramList.add(new BasicNameValuePair("loginmerge", "true"));
			paramList.add(new BasicNameValuePair("splogin", "rate"));
			paramList.add(new BasicNameValuePair("username", TCLiveMod.mod.tb_username));
			paramList.add(new BasicNameValuePair("password", param_password));
			paramList.add(new BasicNameValuePair("mem_pass", "on"));
			paramList.add(new BasicNameValuePair("rsakey", param_rsaKey));
			paramList.add(new BasicNameValuePair("crypttype", "12"));
			paramList.add(new BasicNameValuePair("ppui_logintime", "6523"));
			paramList.add(new BasicNameValuePair("countrycode", ""));
			paramList.add(new BasicNameValuePair("fp_uid", ""));
			paramList.add(new BasicNameValuePair("callback", "parent." + param_callback_login));
			paramList.add(new BasicNameValuePair("codestring", param_codeString));
//			String verifyUrl = String.format(static_verifyUrl, param_token, System.currentTimeMillis(), URLEncoder.encode(veri_code, "utf-8"), param_codeString, param_callback_login);
//			response = httpClient.execute(new HttpGet(verifyUrl));
//			String verifyJson = EntityUtils.toString(response.getEntity());
//			response.close();
//			verifyJson = verifyJson.substring(verifyJson.indexOf("(") + 1, verifyJson.lastIndexOf(")"));
//			jsonObject = jsonParser.parse(verifyJson).getAsJsonObject().getAsJsonObject("errInfo");
//			String errMsg = jsonObject.get("msg").getAsString();
			paramList.add(new BasicNameValuePair("verifycode", veri_code));
			HttpPost httpPost = new HttpPost(static_loginUrl);
			httpPost.setEntity(new UrlEncodedFormEntity(paramList, "utf-8"));
			response = httpClient.execute(httpPost);
			String responseString = EntityUtils.toString(response.getEntity());
			param_codeString = Utils.patternMatch(responseString, "&codeString=(\\w+)&");
			lastErrorCode = Integer.parseInt(Utils.patternMatch(responseString, "err_no=(\\d+)"));
			response.close();
			checkSuccess();
			if (lastErrorCode == 0) hasLogined = true;
		} catch (Exception e) {
			e.printStackTrace();
			lastErrorCode = -1;
		}
	}

}
