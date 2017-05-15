package tclive.tieba;

import java.io.FileInputStream;
import java.security.PublicKey;
import java.util.List;

import javax.crypto.Cipher;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.collect.Lists;

import tclive.TCLiveMod;
import tclive.util.Utils;

public class LoginStatus extends LoginStatusOld {
	
	static String static_tokenUrl = "https://wappass.baidu.com/wp/api/security/antireplaytoken?tpl=tb&v=%d&apiver=v3&tt=%d&callback=%s";
	static String static_checkUrl = "https://wappass.baidu.com/wp/api/login/check?username=%s&apitype=wap&tpl=tb&v=%d&apiver=v3&tt=%d&callback=%s";
	static String static_loginUrl = "https://wappass.baidu.com/wp/api/login?v=%d";
	static String static_codeUrl = "https://wappass.baidu.com/cgi-bin/genimage?";
	
	private static String param_codeString;
	private static String loginJs;
	
	private ScriptEngineManager scriptEngineManager;

	public LoginStatus() {
		super();
	}
	
	private String getRsaKey() {
		String js = Utils.launchRequest(httpClient, new HttpGet("https://passport.baidu.com/passApi/js/uni_login_wrapper.js"));
		js = "https://passport.baidu.com/passApi/js/" + Utils.patternMatch(js, "uni_loginWap_(?:[0-9a-f]+).js", 0);
		js = Utils.launchRequest(httpClient, new HttpGet(js));
		js = "https://passport.baidu.com/passApi/js/" + Utils.patternMatch(js, "loginWap_(?:[0-9a-f]+).js", 0);
		loginJs = js = Utils.launchRequest(httpClient, new HttpGet(js));
		return Utils.patternMatch(js, "t.rsa=\"([A-Z0-9]+)\"");
	}
	
	private String encodePassword(String password, String serverTime) {
		try {
			scriptEngineManager = new ScriptEngineManager(ClassLoader.getSystemClassLoader());
			ScriptEngine engine = scriptEngineManager.getEngineByExtension("js");
			String js = Utils.readFile(new FileInputStream("login.js"));
			engine.eval(js);
			Invocable invoke = (Invocable) engine;
			Object res = invoke.invokeFunction("encryptPass", password, serverTime);
			return (String) res;
		} catch (Exception e) {
			return "";
		}
	}
	
	@Override
	public void tryLogin() {
		try {
			if (!checkCanLogin()) {
				lastErrorCode = -2;
				return;
			}
			httpClient.execute(new HttpGet("https://tieba.baidu.com/")).close();
			String tokenUrl = String.format(static_tokenUrl, System.currentTimeMillis(), System.currentTimeMillis(), Utils.genCallback());
			String text = Utils.removeCallback(Utils.launchRequest(httpClient, new HttpGet(tokenUrl)));
			String param_time = jsonParser.parse(text).getAsJsonObject().get("time").getAsString();
			String checkUrl = String.format(static_checkUrl, TCLiveMod.mod.tb_username, System.currentTimeMillis(), System.currentTimeMillis(), Utils.genCallback());
			text = Utils.removeCallback(Utils.launchRequest(httpClient, new HttpGet(checkUrl)));
			param_codeString = jsonParser.parse(text).getAsJsonObject().getAsJsonObject("data").get("codeString").getAsString();
			if (!param_codeString.isEmpty()) {
				lastErrorCode = 257;
				return;
			}
//			PublicKey publicKey = Utils.getPublicKey(param_rsaKey);
//			Cipher cipher = Cipher.getInstance("RSA");
//			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//			String param_password = new String(Base64.encodeBase64(cipher.doFinal((TCLiveMod.mod.tb_password + param_time).getBytes())));
			String param_password = encodePassword(TCLiveMod.mod.tb_password, param_time);
			String loginUrl = String.format(static_loginUrl, System.currentTimeMillis());
			HttpPost httpPost = new HttpPost(loginUrl);
			List<BasicNameValuePair> paramList = Lists.newArrayList();
			paramList.add(new BasicNameValuePair("codeString", ""));
			paramList.add(new BasicNameValuePair("isphone", "0"));
			paramList.add(new BasicNameValuePair("safeFlag", "0"));
			paramList.add(new BasicNameValuePair("u", "https://tieba.baidu.com/?page=discovery&task=loginLayer&locate=footer_login"));
			paramList.add(new BasicNameValuePair("subpro", "tbwap"));
			paramList.add(new BasicNameValuePair("staticPage", "https://tieba.baidu.com/tb/mobile/sglobal/html/passport/v3Jump.html"));
			paramList.add(new BasicNameValuePair("loginmerge", "1"));
			paramList.add(new BasicNameValuePair("username", TCLiveMod.mod.tb_username));
			paramList.add(new BasicNameValuePair("password", param_password));
			paramList.add(new BasicNameValuePair("verifycode", ""));
			paramList.add(new BasicNameValuePair("timeSpan", "8265"));
			paramList.add(new BasicNameValuePair("apitype", "wap"));
			paramList.add(new BasicNameValuePair("servertime", param_time));
			paramList.add(new BasicNameValuePair("staticpage", "https://tieba.baidu.com/tb/mobile/sglobal/html/passport/v3Jump.html"));
			paramList.add(new BasicNameValuePair("charset", "UTF-8"));
			paramList.add(new BasicNameValuePair("tpl", "tb"));
			paramList.add(new BasicNameValuePair("callback", "parent." + Utils.genCallback()));
			httpPost.setEntity(new UrlEncodedFormEntity(paramList, "utf-8"));
			String response = Utils.launchRequest(httpClient, httpPost);
			param_codeString = Utils.patternMatch(response, "&codeString=(\\w+)&");
			lastErrorCode = Integer.parseInt(Utils.patternMatch(response, "err_no=(\\d+)"));
			checkSuccess();
			if (lastErrorCode == 0) hasLogined = true;
		} catch (Exception e) {
			e.printStackTrace();
			lastErrorCode = -1;
		}
	}
	
	@Override
	public void loadVerifyCodePicture() {
		try {
			String codeUrl = static_codeUrl + param_codeString + "&v=" + System.currentTimeMillis();
			Utils.downLoadFromUrl(codeUrl, "veri_code.png", "./");
		} catch (Exception e) {
			TCLiveMod.LOG.warn("Unable to download verify code picture.");
		}
	}
	
	@Override
	public void tryLoginWithCode(String veri_code) {
		try {
			if (!checkCanLogin()) {
				lastErrorCode = -2;
				return;
			}
			httpClient.execute(new HttpGet("https://tieba.baidu.com/")).close();
			String tokenUrl = String.format(static_tokenUrl, System.currentTimeMillis(), System.currentTimeMillis(), Utils.genCallback());
			String text = Utils.removeCallback(Utils.launchRequest(httpClient, new HttpGet(tokenUrl)));
			String param_time = jsonParser.parse(text).getAsJsonObject().get("time").getAsString();
			String param_password = encodePassword(TCLiveMod.mod.tb_password, param_time);
			String loginUrl = String.format(static_loginUrl, System.currentTimeMillis());
			HttpPost httpPost = new HttpPost(loginUrl);
			List<BasicNameValuePair> paramList = Lists.newArrayList();
			paramList.add(new BasicNameValuePair("codeString", ""));
			paramList.add(new BasicNameValuePair("isphone", "0"));
			paramList.add(new BasicNameValuePair("safeFlag", "0"));
			paramList.add(new BasicNameValuePair("u", "https://tieba.baidu.com/?page=discovery&task=loginLayer&locate=footer_login"));
			paramList.add(new BasicNameValuePair("subpro", "tbwap"));
			paramList.add(new BasicNameValuePair("staticPage", "https://tieba.baidu.com/tb/mobile/sglobal/html/passport/v3Jump.html"));
			paramList.add(new BasicNameValuePair("loginmerge", "1"));
			paramList.add(new BasicNameValuePair("username", TCLiveMod.mod.tb_username));
			paramList.add(new BasicNameValuePair("password", param_password));
			paramList.add(new BasicNameValuePair("verifycode", veri_code));
			paramList.add(new BasicNameValuePair("timeSpan", "8265"));
			paramList.add(new BasicNameValuePair("apitype", "wap"));
			paramList.add(new BasicNameValuePair("servertime", param_time));
			paramList.add(new BasicNameValuePair("staticpage", "https://tieba.baidu.com/tb/mobile/sglobal/html/passport/v3Jump.html"));
			paramList.add(new BasicNameValuePair("charset", "UTF-8"));
			paramList.add(new BasicNameValuePair("tpl", "tb"));
			paramList.add(new BasicNameValuePair("callback", "parent." + Utils.genCallback()));
			paramList.add(new BasicNameValuePair("vcodestr", param_codeString));
			httpPost.setEntity(new UrlEncodedFormEntity(paramList, "utf-8"));
			String response = Utils.launchRequest(httpClient, httpPost);
			param_codeString = Utils.patternMatch(response, "&codeString=(\\w+)&");
			lastErrorCode = Integer.parseInt(Utils.patternMatch(response, "err_no=(\\d+)"));
			checkSuccess();
			if (lastErrorCode == 0) hasLogined = true;
		} catch (Exception e) {
			e.printStackTrace();
			lastErrorCode = -1;
		}
	}
}
