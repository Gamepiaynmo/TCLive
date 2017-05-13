package tclive.tieba;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tclive.TCLiveMod;
import tclive.util.Utils;

public class MakeReply {

	static String static_replyUrl = "https://tieba.baidu.com/f/commit/post/add";
	static String static_tieziUrl = "https://tieba.baidu.com/p/";
	static String static_imgtbsUrl = "https://tieba.baidu.com/dc/common/imgtbs";
	static String static_uploadUrl = "https://uploadphotos.baidu.com/upload/pic?tbs=%s&fid=%s&save_yun_album=1";

	static String param_mouse = "123,119,119,98,124,125,127,122,71,127,98,126,98,127,98,126,98,127,98,126,71,119,121,118,122,71,127,123,125,120,98,118,120,122,14944138859780";
	static String param_image = "[img+pic_type=%d+width=%d+height=%d]%s[/img]";
	
	String param_fid = "";
	
	int lastError;
	
	public MakeReply() {
		new Thread(this::getFid).start();
	}
	
	void getFid() {
		try {
			CloseableHttpClient httpClient = TCLiveMod.mod.httpClient;
			CloseableHttpResponse response = httpClient.execute(new HttpGet(static_tieziUrl + TCLiveMod.mod.tb_livetid));
			String tiezi = EntityUtils.toString(response.getEntity());
			param_fid = Utils.patternMatch(tiezi, "fid:'(\\w+)'");
		} catch (Exception e) {
			lastError = -1;
		}
	}
	
	public String uploadImage(File image) {
		try {
			if (TCLiveMod.mod.tb_livetid.isEmpty()) {
				lastError = -3;
				return null;
			}
			if (param_fid.isEmpty())
				getFid();
			CloseableHttpClient httpClient = TCLiveMod.mod.httpClient;
			JsonParser jsonParser = TCLiveMod.mod.jsonParser;
			CloseableHttpResponse response = httpClient.execute(new HttpGet(static_imgtbsUrl));
			String imgtbsJson = EntityUtils.toString(response.getEntity());
			response.close();
			String param_imgtbs = Utils.patternMatch(imgtbsJson, "\"tbs\":\"(\\w+)\"");
			String uploadUrl = String.format(static_uploadUrl, param_imgtbs, param_fid);
			httpClient.execute(new HttpOptions(uploadUrl)).close();
			HttpPost httpPost = new HttpPost(uploadUrl);
			httpPost.setEntity(MultipartEntityBuilder.create().addPart("file", new FileBody(image)).build());
			response = httpClient.execute(httpPost);
			JsonObject jsonObject = jsonParser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject().getAsJsonObject("info");
			String imageUrl = jsonObject.get("pic_water").getAsString();
			int width = jsonObject.get("fullpic_width").getAsInt();
			int height = jsonObject.get("fullpic_height").getAsInt();
			int type = jsonObject.get("pic_type").getAsInt();
			response.close();
			lastError = jsonObject.get("err_no").getAsInt();
			float scale = calcScale(width, height);
			return URLEncoder.encode(String.format(param_image, type, (int) (width * scale), (int) (height * scale), imageUrl), "utf-8").replaceAll("%2B", "+");
		} catch (Exception e) {
			lastError = -1;
			return null;
		}
	}
	
	private float calcScale(int width, int height) {
		float res = 1.0f;
		if (width > 570) res = Math.min(res, 560.0f / width);
		if (height > 570) res = Math.min(res, 560.0f / height);
		return res;
	}
	
	public String getEmotionString(int index) {
		try {
			if (index > 0 && index <= 50)
				return URLEncoder.encode(String.format("[emotion+pic_type=1+width=30+height=30]//tb2.bdstatic.com/tb/editor/images/face/i_f%2d.png?t=20140803[/emotion]", index), "utf-8").replaceAll("%2B", "+");
			else return String.format("@[emo%02d]@", index);
		} catch (UnsupportedEncodingException e) {
			return String.format("@[emo%02d]@", index);
		}
	}
	
	public void tryPost(String content) {
		try {
			if (!TCLiveMod.mod.loginStatus.hasLogined()) {
				TCLiveMod.mod.loginStatus.tryLogin();
				Utils.handleLoginErrorCode(TCLiveMod.mod.loginStatus.getLastError());
			}
			post(content);
		} catch (Exception e) {
			lastError = -1;
		}
	}
	
	public int getLastError() { return lastError; }
	
	void post(String content) throws Exception {
		if (TCLiveMod.mod.tb_livetid.isEmpty()) {
			lastError = -3;
			return;
		}
		CloseableHttpClient httpClient = TCLiveMod.mod.httpClient;
		JsonParser jsonParser = TCLiveMod.mod.jsonParser;
		CloseableHttpResponse response = httpClient.execute(new HttpGet(static_tieziUrl + TCLiveMod.mod.tb_livetid));
		String tiezi = EntityUtils.toString(response.getEntity());
		response.close();
		param_fid = Utils.patternMatch(tiezi, "fid:'(\\w+)'");
		String param_kw = Utils.patternMatch(tiezi, "kw:'([^']+)'");
		String param_tbs = Utils.patternMatch(tiezi, "tbs: '(\\w+)'");
		HttpPost httpPost = new HttpPost(static_replyUrl);
		List<BasicNameValuePair> paramList = Lists.newArrayList();
		paramList.add(new BasicNameValuePair("__type__", "reply"));
		paramList.add(new BasicNameValuePair("basilisk", "1"));
		//paramList.add(new BasicNameValuePair("content", String.format(param_image, type, width, height, imageUrl)));
		paramList.add(new BasicNameValuePair("fid", param_fid));
		paramList.add(new BasicNameValuePair("files", "[]"));
		paramList.add(new BasicNameValuePair("floor_num", "1"));
		paramList.add(new BasicNameValuePair("ie", "utf-8"));
		paramList.add(new BasicNameValuePair("kw", param_kw));
		paramList.add(new BasicNameValuePair("mouse_pwd", param_mouse));
		paramList.add(new BasicNameValuePair("mouse_pwd_isclick", "0"));
		paramList.add(new BasicNameValuePair("mouse_pwd_t", String.valueOf(System.currentTimeMillis())));
		paramList.add(new BasicNameValuePair("rich_text", "1"));
		paramList.add(new BasicNameValuePair("tbs", param_tbs));
		paramList.add(new BasicNameValuePair("tid", TCLiveMod.mod.tb_livetid));
		paramList.add(new BasicNameValuePair("vcode_md5", ""));
		httpPost.setEntity(new StringEntity(EntityUtils.toString(new UrlEncodedFormEntity(paramList, "utf-8")) + "&content=" + content));
		response = httpClient.execute(httpPost);
		String responseString = EntityUtils.toString(response.getEntity());
		lastError = Integer.parseInt(Utils.patternMatch(responseString, "\"err_code\":(\\d+)"));
		response.close();
	}

}
