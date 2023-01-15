package org.example;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.http.HttpUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ximo.lyy
 * @package ${PROJECT_NAME}
 * @date ${YEAR}/${MONTH}/${DAY}
 */
public class Main {

    private static final String PATH =
        "/Users/liangyueyong/Library/Mobile Documents/iCloud~md~obsidian/Documents/YYLiang";

    public static void main(String[] args) {
        List<File> files = listAllMDFile();
        List<String> url = new ArrayList<>();
        files.forEach(file -> {
            // 默认UTF-8 编码，可以在构造中传入第二个参数做为编码
            FileReader fileReader = new FileReader(file.getPath());
            List<String> urls = getAllUrlsFromContent(fileReader.readString());
            // 筛选出所有的包含新浪图床的链接

            urls.stream().filter(StringUtils::isNotBlank).filter(s -> StringUtils.contains(s, "sina"))
                .forEach(url::add);
        });
        System.out.println(url.get(1));
        HttpUtil.downloadFileFromUrl(url.get(1), "/Users/liangyueyong/Downloads");
    }

    /**
     * 筛选出所有的markdown文件
     */
    public static List<File> listAllMDFile() {
        List<File> files = FileUtil.loopFiles(PATH);
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
}