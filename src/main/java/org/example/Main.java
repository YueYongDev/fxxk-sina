package org.example;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.thread.ThreadUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
public class Main {

    /**
     * 初始化线程池，同时执行5个线程
     */
    private static ExecutorService executor = ThreadUtil.newExecutor(5);

    private static final String VAULT_PATH =
            "/Users/yueyong/Downloads/YYLiang";

    private static final String IMG_DOWNLOAD_PATH = "/Users/yueyong/Downloads/image";

    public static void main(String[] args) {
        List<File> files = listAllMDFile();
        List<String> sinaImgUrls = new ArrayList<>();
        files.forEach(file -> {
            // 默认UTF-8 编码，可以在构造中传入第二个参数做为编码
            FileReader fileReader = new FileReader(file.getPath());
            List<String> urls = getAllUrlsFromContent(fileReader.readString());
            // 筛选出所有的包含新浪图床的链接
            urls.stream()
                    .filter(StringUtils::isNotBlank)
                    .filter(s -> StringUtils.contains(s, "sina"))
                    .forEach(sinaImgUrls::add);
        });

        List<String> errorImages = new ArrayList<>();
        for (int i = 0; i < sinaImgUrls.size(); i++) {
            String imgUrl = sinaImgUrls.get(i);
            if (StringUtils.containsAny(imgUrl,"ws1.","ws2","ws3","ws4")){
            }
            String path = IMG_DOWNLOAD_PATH + "/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
            try {
                download(imgUrl, path);
                System.out.println("第" + i + "张下载完成");
            } catch (IOException e) {
                System.out.println("第" + i + "张下载失败");
                errorImages.add(imgUrl);
                System.out.println(e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(errorImages)) {
            errorImages.forEach(System.out::println);
        }
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
}