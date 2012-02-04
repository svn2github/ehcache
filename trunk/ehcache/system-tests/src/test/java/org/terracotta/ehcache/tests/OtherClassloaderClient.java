/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_5;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.classloader.ClassLoaderAwareCache;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.terracotta.api.ClusteringToolkit;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class OtherClassloaderClient extends ClientBase {

  public static void main(String[] args) {
    new OtherClassloaderClient(args).run();
  }

  public OtherClassloaderClient(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache c, ClusteringToolkit toolkit) throws Throwable {
    // Construct Value instances from a foreign classloader
    ClassLoader otherClassLoader = createClassLoader();
    Ehcache cache = new ClassLoaderAwareCache(c, otherClassLoader);

    Class<? extends Valueable> valueClass = (Class<? extends Valueable>) otherClassLoader
        .loadClass("org.terracotta.ehcache.tests.Value");

    // put more elements than fit in memory (maxElementsInMemory = 1)
    for (int i = 0; i < 5; i++) {
      Integer k = Integer.valueOf(i);
      Valueable v = valueClass.newInstance();
      v.setValue(i);
      cache.put(new Element(k, v));
    }

    // get all elements back out
    for (int i = 0; i < 5; i++) {
      Integer k = Integer.valueOf(i);

      Element e = cache.get(k);
      Valueable v = (Valueable) e.getValue();
      if (i != v.value()) { throw new Exception("Expected " + i + " but got " + v.value()); }
    }

  }

  protected ClassLoader createClassLoader() {
    try {
      byte[] valueBytes = createValueClass();

      Map<String, byte[]> classes = new HashMap<String, byte[]>();
      classes.put("org.terracotta.ehcache.tests.Value", valueBytes);

      return new ByteClassLoader(new URL[] {}, getClass().getClassLoader(), classes);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected byte[] createValueClass() throws Exception {
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;

    cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "org/terracotta/ehcache/tests/Value", null, "java/lang/Object",
             new String[] { "org/terracotta/ehcache/tests/Valueable" });

    cw.visitSource("Value.java", null);

    fv = cw.visitField(ACC_PRIVATE, "v", "I", null, null);
    fv.visitEnd();
    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(9, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(11, l1);
      mv.visitInsn(RETURN);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLocalVariable("this", "Lorg/terracotta/ehcache/tests/Value;", null, l0, l2, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "value", "()I", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(18, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/terracotta/ehcache/tests/Value", "v", "I");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lorg/terracotta/ehcache/tests/Value;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "setValue", "(I)V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(22, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitFieldInsn(PUTFIELD, "org/terracotta/ehcache/tests/Value", "v", "I");
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(23, l1);
      mv.visitInsn(RETURN);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLocalVariable("this", "Lorg/terracotta/ehcache/tests/Value;", null, l0, l2, 0);
      mv.visitLocalVariable("v", "I", null, l0, l2, 1);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  public static class ByteClassLoader extends URLClassLoader {
    private final Map<String, byte[]> extraClassDefs;

    public ByteClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
      super(urls, parent);
      this.extraClassDefs = new HashMap<String, byte[]>(extraClassDefs);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
      byte[] classBytes = this.extraClassDefs.remove(name);
      if (classBytes != null) { return defineClass(name, classBytes, 0, classBytes.length); }
      return super.findClass(name);
    }
  }
}
