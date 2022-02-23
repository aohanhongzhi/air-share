package hxy.dragon.entity.enums;

/**
 * The interface Enumerator.
 * @author eric
 */
//@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public interface Enumerator {
    /**
     * Code integer.
     *
     * @return the integer
     */
    Integer code();

    /**
     * Description string.
     *
     * @return the string
     */
    String description();

    /** 反序列化需要实现
     * @return
     */
    String name();
}