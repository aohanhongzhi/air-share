package hxy.dragon.dao.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import hxy.dragon.entity.enums.FileTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author eric
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class FileModel {
    /**
     * 文件的uuid事主键
     * 主键，存在就更新，不存在就插入
     */
    @TableId(type = IdType.AUTO)
    private String fileUuid;

    /**
     * 文件md5值
     */
    private String fileMd5;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件存储路径
     */
    private String filePath;


    /**
     * 文件大小 Byte
     */
    private long fileSize = 0;

    /**
     * 文档类型
     */
    private FileTypeEnum fileType;
}
