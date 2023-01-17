package org.example;

import cn.hutool.Hutool;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.thread.ThreadUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ximo.lyy
 * @package ${PROJECT_NAME}
 * @date ${YEAR}/${MONTH}/${DAY}
 */
@Slf4j
public class Main {
    /**
     * 初始化线程池，同时执行5个线程
     */
    private static ExecutorService executor = ThreadUtil.newExecutor(5);

    private static final String VAULT_PATH = "/Users/liangyueyong/Downloads/YYLiang";

    private static final String IMG_DOWNLOAD_PATH = "/Users/liangyueyong/Downloads/image";

    //...生成上传凭证，然后准备上传
    private static final String AK = "your access key";

    private static final String SK = "your secret key";

    private static final String BUCKET = "your BUCKET name";

    private static final String BASE_DOMAIN = "";

    private static final List<String> ERROR_URL = Lists.newArrayList();
    private static final Map<String, String> URL_MAP = Maps.newHashMap();

    public static void main(String[] args) {
        List<File> files = listAllMDFile();
        files.forEach(file -> {
            // 默认UTF-8 编码，可以在构造中传入第二个参数做为编码
            FileReader fileReader = new FileReader(file.getPath());
            List<String> urls = getAllUrlsFromContent(fileReader.readString());
            // 筛选出所有的包含新浪图床的链接
            urls.stream()
                .filter(StringUtils::isNotBlank)
                .filter(s -> StringUtils.contains(s, "sina"))
                .forEach(s -> URL_MAP.put(s, StringUtils.EMPTY));
        });
        URL_MAP.forEach((originUrl, replaceUrl) -> {
            String path = IMG_DOWNLOAD_PATH + "/" + originUrl.substring(originUrl.lastIndexOf("/") + 1);
            try {
                download(originUrl, path);
                log.info("download success:{}", originUrl);
                String uploadUrl = upload(path);
                if (StringUtils.isNotBlank(uploadUrl)) {
                    log.info("upload success, new url:{}", uploadUrl);
                    URL_MAP.put(originUrl, uploadUrl);
                }
            } catch (Throwable e) {
                ERROR_URL.add(originUrl);
                log.error("handle error:{}, errMsg:{}", originUrl, e.getMessage());
            }
        });

        files.forEach(file -> {
            try {
                // 默认UTF-8 编码，可以在构造中传入第二个参数做为编码
                FileReader fileReader = new FileReader(file.getPath());
                String content = fileReader.readString();
                String replaceContent = replaceUrl(content, URL_MAP);
                FileWriter writer = new FileWriter(file.getPath());
                writer.write(replaceContent);
            } catch (Throwable e) {
                log.error("write file error, errorMsg:{}", e.getMessage());
            }
        });
    }

    /**
     * 筛选出所有的markdown文件
     */
    public static List<File> listAllMDFile() {
        List<File> files = FileUtil.loopFiles(VAULT_PATH);
        return files.stream().filter(Objects::nonNull).filter(File::isFile)
            .filter(file -> StringUtils.endsWith(file.getName(), ".md")).collect(Collectors.toList());
    }

    /**
     * 获取一段文本内容里的所有url
     *
     * @param content 文本内容
     * @return 所有的url
     */
    public static List<String> getAllUrlsFromContent(String content) {
        List<String> urls = new ArrayList<>();
        Pattern pattern = Pattern.compile(
            "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" + "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov"
                + "|mil|biz|info|mobi|name|aero|jobs|museum" + "|travel|[a-z]{2}))(:[\\d]{1,5})?"
                + "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" + "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?"
                + "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" + "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?"
                + "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" + "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    public static void download(String urlString, String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            return;
        }
        URL url = null;
        OutputStream os = null;
        InputStream is = null;
        try {
            url = new URL(urlString);
            URLConnection con = url.openConnection();
            // 输入流
            is = con.getInputStream();
            // 1K的数据缓冲
            byte[] bs = new byte[1024];
            // 读取到的数据长度
            int len;
            // 输出的文件流
            os = Files.newOutputStream(Paths.get(fileName));
            // 开始读取
            while ((len = is.read(bs)) != -1) {
                os.write(bs, 0, len);
            }
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    public static String upload(String localFilePath) throws QiniuException {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());
        UploadManager uploadManager = new UploadManager(cfg);
        //默认不指定key的情况下，以文件内容的hash值作为文件名
        String key = null;

        Auth auth = Auth.create(AK, SK);
        String upToken = auth.uploadToken(BUCKET);

        Response response = uploadManager.put(localFilePath, key, upToken);
        //解析上传成功的结果
        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
        System.out.println(putRet.key);
        System.out.println(putRet.hash);
        return BASE_DOMAIN + putRet.key;
    }

    /**
     * 替换所有的图片链接
     */
    private static String replaceUrl(String content, Map<String, String> urlMap) {
        for (Map.Entry<String, String> entry : urlMap.entrySet()) {
            String oldUrl = entry.getKey();
            String newUrl = entry.getValue();
            if (StringUtils.isBlank(newUrl)) {
                continue;
            }
            return RegExUtils.replaceAll(content, oldUrl, newUrl);
        }
        return content;
    }
}