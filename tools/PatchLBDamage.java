import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class PatchLBDamage {
    private static final String DAMAGE_TRACKER =
            "com/ribbu/lbdamage/DamageTracker.class";
    private static final String ENTITY_IDS =
            "com/ribbu/lbdamage/LBDamageEntityIds.class";
    private static final String ATTACK_TARGETS =
            "com/ribbu/lbdamage/LBDamageAttackTargets.class";
    private static final String LOCAL_ATTACK =
            "com/ribbu/lbdamage/mixin/LocalAttackMixin.class";
    private static final String OLD_OWNER =
            "net/minecraft/world/entity/EntityType";
    private static final String NEW_OWNER =
            "net/minecraft/world/entity/EntityTypes";
    private static final String DISPLAY_ENTITY =
            "net/minecraft/world/entity/Display$TextDisplay";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: PatchLBDamage <input.jar> <output.jar>");
        }
        patch(Path.of(args[0]), Path.of(args[1]));
    }

    private static void patch(Path input, Path output) throws IOException {
        Files.deleteIfExists(output);
        boolean changed = false;
        try (ZipFile zip = new ZipFile(input.toFile());
             OutputStream fileOut = Files.newOutputStream(output);
             ZipOutputStream out = new ZipOutputStream(fileOut)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry source = entries.nextElement();
                ZipEntry target = new ZipEntry(source.getName());
                target.setTime(source.getTime());
                if (source.getMethod() == ZipEntry.STORED) {
                    target.setMethod(ZipEntry.DEFLATED);
                }
                out.putNextEntry(target);
                try (InputStream in = zip.getInputStream(source)) {
                    byte[] data = in.readAllBytes();
                    if (DAMAGE_TRACKER.equals(source.getName())) {
                        byte[] patched = patchClass(data);
                        changed = !java.util.Arrays.equals(data, patched);
                        data = patched;
                    } else if (LOCAL_ATTACK.equals(source.getName())) {
                        byte[] patched = patchLocalAttackClass(data);
                        changed = !java.util.Arrays.equals(data, patched) || changed;
                        data = patched;
                    }
                    out.write(data);
                }
                out.closeEntry();
            }
            ZipEntry helper = new ZipEntry(ENTITY_IDS);
            out.putNextEntry(helper);
            out.write(helperClass());
            out.closeEntry();

            ZipEntry targets = new ZipEntry(ATTACK_TARGETS);
            out.putNextEntry(targets);
            out.write(readClassResource(ATTACK_TARGETS));
            out.closeEntry();
        }
        if (!changed) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("DamageTracker.class did not contain the expected field reference");
        }
    }

    private static byte[] patchClass(byte[] input) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor remapper = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    private boolean markerConstructor;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if ("hasPlayerAttackContext".equals(name)) {
                            Label continueLabel = new Label();
                            delegate.visitVarInsn(Opcodes.ALOAD, 0);
                            delegate.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/ribbu/lbdamage/LBDamageAttackTargets",
                                    "matches",
                                    "(Lnet/minecraft/world/entity/LivingEntity;)Z",
                                    false
                            );
                            delegate.visitJumpInsn(Opcodes.IFEQ, continueLabel);
                            delegate.visitInsn(Opcodes.ICONST_1);
                            delegate.visitInsn(Opcodes.IRETURN);
                            delegate.visitLabel(continueLabel);
                        }
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                        if (opcode == Opcodes.GETSTATIC && OLD_OWNER.equals(owner)
                                && "TEXT_DISPLAY".equals(fieldName)) {
                            owner = NEW_OWNER;
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String descriptor, boolean isInterface) {
                        if (markerConstructor
                                && opcode == Opcodes.INVOKEVIRTUAL
                                && "net/minecraft/client/multiplayer/ClientLevel".equals(owner)
                                && "addEntity".equals(name)
                                && "(Lnet/minecraft/world/entity/Entity;)V".equals(descriptor)) {
                            // 26.2 no longer auto-assigns ids in Entity's constructor.
                            // Allocate a private negative id before ClientLevel.addEntity()
                            // performs its initial getId()/removeEntity lookup.
                            delegate.visitVarInsn(Opcodes.ALOAD, 14);
                            delegate.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/ribbu/lbdamage/LBDamageEntityIds",
                                    "next",
                                    "()I",
                                    false
                            );
                            delegate.visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    "net/minecraft/world/entity/Entity",
                                    "setId",
                                    "(I)V",
                                    false
                            );
                            markerConstructor = false;
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        if (opcode == Opcodes.INVOKESPECIAL
                                && DISPLAY_ENTITY.equals(owner)
                                && "<init>".equals(name)) {
                            markerConstructor = true;
                        }
                    }
                };
            }
        };
        reader.accept(remapper, 0);
        return writer.toByteArray();
    }

    private static byte[] patchLocalAttackClass(byte[] input) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"lbdamage$localAttack".equals(name)) {
                    return delegate;
                }
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 2);
                        super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "com/ribbu/lbdamage/LBDamageAttackTargets",
                                "mark",
                                "(Lnet/minecraft/world/entity/Entity;)V",
                                false
                        );
                    }

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        // The second argument is the attacked entity. The original
                        // 26.2 build recorded the attacker's position instead.
                        if (opcode == Opcodes.ALOAD && var == 1) {
                            var = 2;
                        }
                        super.visitVarInsn(opcode, var);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "net/minecraft/world/entity/player/Player".equals(owner)
                                && ("getX".equals(methodName)
                                || "getY".equals(methodName)
                                || "getZ".equals(methodName))) {
                            owner = "net/minecraft/world/entity/Entity";
                        }
                        super.visitMethodInsn(opcode, owner, methodName, descriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] readClassResource(String path) throws IOException {
        try (InputStream in = PatchLBDamage.class.getResourceAsStream("/" + path)) {
            if (in == null) {
                throw new IOException("Missing helper class resource: " + path);
            }
            return in.readAllBytes();
        }
    }

    private static byte[] helperClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V25, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "com/ribbu/lbdamage/LBDamageEntityIds", null, "java/lang/Object", null);

        var field = writer.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "NEXT",
                "Ljava/util/concurrent/atomic/AtomicInteger;",
                null,
                null
        );
        field.visitEnd();

        var ctor = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(1, 1);
        ctor.visitEnd();

        var clinit = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        clinit.visitTypeInsn(Opcodes.NEW, "java/util/concurrent/atomic/AtomicInteger");
        clinit.visitInsn(Opcodes.DUP);
        clinit.visitLdcInsn(Integer.MIN_VALUE);
        clinit.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/util/concurrent/atomic/AtomicInteger",
                "<init>",
                "(I)V",
                false
        );
        clinit.visitFieldInsn(
                Opcodes.PUTSTATIC,
                "com/ribbu/lbdamage/LBDamageEntityIds",
                "NEXT",
                "Ljava/util/concurrent/atomic/AtomicInteger;"
        );
        clinit.visitInsn(Opcodes.RETURN);
        clinit.visitMaxs(3, 0);
        clinit.visitEnd();

        var next = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "next", "()I", null, null);
        next.visitCode();
        next.visitFieldInsn(
                Opcodes.GETSTATIC,
                "com/ribbu/lbdamage/LBDamageEntityIds",
                "NEXT",
                "Ljava/util/concurrent/atomic/AtomicInteger;"
        );
        next.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/util/concurrent/atomic/AtomicInteger",
                "getAndIncrement",
                "()I",
                false
        );
        next.visitInsn(Opcodes.IRETURN);
        next.visitMaxs(1, 0);
        next.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }
}
