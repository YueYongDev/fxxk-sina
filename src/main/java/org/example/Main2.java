package org.example;

import cn.hutool.core.io.file.FileReader;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.example.Main.getAllUrlsFromContent;
import static org.example.Main.listAllMDFile;

/**
 * @author ximo.lyy
 * @package fxxk-sina
 * @date 2023/01/22
 */
public class Main2 {
    private static final Map<String, String> URL_MAP = Maps.newHashMap();
    public static void main(String[] args) {

        List<File> files = listAllMDFile();
        Set<String> filePathList = Sets.newHashSet();
        files.forEach(file -> {
            // 默认UTF-8 编码，可以在构造中传入第二个参数做为编码
            FileReader fileReader = new FileReader(file.getPath());
            List<String> urls = getAllUrlsFromContent(fileReader.readString());
            // 筛选出所有的包含新浪图床的链接
            urls.stream()
                .filter(StringUtils::isNotBlank)
                .filter(s -> StringUtils.containsAny(s, "sina", "jianshu", "user-gold-cdn"))
                .forEach(s ->filePathList.add(file.getPath()));
        });
        filePathList.forEach(System.out::println);
    }
}
