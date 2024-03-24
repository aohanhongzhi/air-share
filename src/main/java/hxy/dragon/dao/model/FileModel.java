package hxy.dragon.dao.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author eric
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class FileModel {

    private Integer id;
    /**
     * 文件的uuid是主键。 air-share是随意新建的，airshare-plus是文件md5值
     * 主键，存在就更新，不存在就插入
     */
    @TableId(type = IdType.NONE)
    private String fileUuid;

    /**
     * 文件md5值，目前新的 air-share-plus支持 md5，旧版本没有md5
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
     * 访问域名
     */
    private String serverName;


    private Date createTime;

}
