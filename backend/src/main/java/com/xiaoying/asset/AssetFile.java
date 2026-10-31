package com.xiaoying.asset;

import org.springframework.core.io.Resource;

/** 读取素材文件的结果：文件内容和上传时识别出的真实类型。 */
public record AssetFile(Resource resource, String mimeType) {}
