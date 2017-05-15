package tclive.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.common.collect.Lists;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import tclive.TCLiveMod;
import tclive.gui.InputVerifyCode;

public class Utils {

	public static Random rand = new Random();
	
	public static String genGid() {
		char[] gid = "xxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".toCharArray();
		for (int i = 0; i < gid.length; i++) {
			int n = rand.nextInt(16);
			n = gid[i] == 'x' ? n : gid[i] == 'y' ? 3 & n | 8 : gid[i];
			gid[i] = n == gid[i] ? gid[i] : Integer.toString(n, 16).charAt(0);
		}
		return String.valueOf(gid).toUpperCase();
	}

	public static String genCallback() {
		return "bd__cbs__" + Integer.toString((int) Math.floor(2147483648L * rand.nextDouble()), 36);
	}
	
	public static String launchRequest(CloseableHttpClient httpClient, HttpUriRequest request) {
		try {
			CloseableHttpResponse response = httpClient.execute(request);
			String text = EntityUtils.toString(response.getEntity());
			response.close();
			return text;
		} catch (Exception e) {
			return "";
		}
	}
	
	public static String removeCallback(String text) {
		return text.substring(text.indexOf("(") + 1, text.lastIndexOf(")"));
	}

	public static PublicKey getPublicKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.decodeBase64(key);

		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return publicKey;
	}

	public static String patternMatch(String text, String pattern) {
		Pattern localPattern = Pattern.compile(pattern);
		Matcher localMatcher = localPattern.matcher(text);
		if (localMatcher.find()) {
			return localMatcher.group(1);
		}
		return null;
	}
	
	public static String patternMatch(String text, String pattern, int group) {
		Pattern localPattern = Pattern.compile(pattern);
		Matcher localMatcher = localPattern.matcher(text);
		if (localMatcher.find()) {
			return localMatcher.group(group);
		}
		return null;
	}
	
	public static List<String> patternMatches(String text, String pattern) {
		Pattern localPattern = Pattern.compile(pattern);
		Matcher localMatcher = localPattern.matcher(text);
		List<String> matches = Lists.newArrayList();
		while (localMatcher.find())
			matches.add(localMatcher.group());
		return matches;
	}
	
	public static String[] patternSplit(String text, String pattern) {
		Pattern localPattern = Pattern.compile(pattern);
		return localPattern.split(text);
	}
	
	public static String readFile(InputStream is) throws IOException {
		String line;
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		line = reader.readLine();
		while (line != null) {
			buffer.append(line);
			buffer.append("\n");
			line = reader.readLine();
		}
		reader.close();
		is.close();
		return buffer.toString();
	}
	
	public static void downLoadFromUrl(String urlStr, String fileName, String savePath) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		InputStream inputStream = conn.getInputStream();
		byte[] getData = readInputStream(inputStream);

		File saveDir = new File(savePath);
		if (!saveDir.exists()) {
			saveDir.mkdirs();
		}
		File file = new File(saveDir + File.separator + fileName);
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(getData);
		if (fos != null) {
			fos.close();
		}
		if (inputStream != null) {
			inputStream.close();
		}
	}
	
	static byte[] readInputStream(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[1024];
		int len = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((len = inputStream.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		bos.close();
		return bos.toByteArray();
	}
	
	public static void handleLoginErrorCode(int code) {
		switch (code) {
		case 0: sendMessageTranslate("live.loginsucceed"); return;
		case 257:
			TCLiveMod.mod.loginStatus.loadVerifyCodePicture();
			TCLiveMod.mc.displayGuiScreen(new InputVerifyCode(true));
			return;
		case 6:
			TCLiveMod.mod.loginStatus.loadVerifyCodePicture();
			TCLiveMod.mc.displayGuiScreen(new InputVerifyCode(false));
			return;
		case 500001:
			TCLiveMod.mod.loginStatus.loadVerifyCodePicture();
			TCLiveMod.mc.displayGuiScreen(new InputVerifyCode(true));
			return;
		case 500002:
			TCLiveMod.mod.loginStatus.loadVerifyCodePicture();
			TCLiveMod.mc.displayGuiScreen(new InputVerifyCode(false));
			return;
		default: sendMessageTranslate(getLoginFailReason(code)); return;
		}
	}
	
	public static void handlePostErrorCode(int code) {
		switch (code) {
		case 0: sendMessageActionBarTranslate("live.postsucceed"); return;
		default: sendMessageActionBarTranslate(getPostFailReason(code)); return;
		}
	}
	
	public static String getLoginFailReason(int reason) {
		String key = "live.failreason." + reason;
		return I18n.format("live.loginfailed") + " : " + (I18n.hasKey(key) ? I18n.format(key) : I18n.format("live.failreason.unknown", reason));
	}
	
	public static String getPostFailReason(int reason) {
		String key = "live.failreason." + reason;
		return I18n.format("live.postfailed") + " : " + (I18n.hasKey(key) ? I18n.format(key) : I18n.format("live.failreason.unknown", reason));
	}
	
	public static void sendMessage(String text) {
		text = "[TCLive] " + text;
		try {
			TCLiveMod.mc.player.sendMessage(new TextComponentString(text));
		} catch (Exception e) {
			TCLiveMod.LOG.info(text);
		}
	}
	
	public static void sendMessageActionBar(String text) {
		text = "[TCLive] " + text;
		try {
			TCLiveMod.mc.ingameGUI.setOverlayMessage(text, false);
		} catch (Exception e) {
			TCLiveMod.LOG.info(text);
		}
	}
	
	public static void sendMessageTranslate(String text, Object ... param) {
		sendMessage(I18n.format(text, param));
	}
	
	public static void sendMessageActionBarTranslate(String text, Object ... param) {
		sendMessageActionBar(I18n.format(text, param));
	}
}
