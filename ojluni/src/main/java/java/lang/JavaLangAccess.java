package java.lang;

/**
 * @hide
 */
public final class JavaLangAccess {
  private JavaLangAccess() {
  }

  /**
   * @hide
   */
  public static int getStringHash32(String string) {
      return string.hash32();
  }

  /**
   * @hide
   */
  public static <E extends Enum<E>>
          E[] getEnumConstantsShared(Class<E> klass) {
      return klass.getEnumConstantsShared();
  }
}
