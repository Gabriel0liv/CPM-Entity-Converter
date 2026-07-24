package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.zip.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CpmArtifactMutationMatrixTest {
  private record MutationCase(String id, UnaryOperator<byte[]> mutation, boolean success, String code) {}

  @Test
  void realMutationsDifferFromTheS003BaseAndHaveExpectedDiagnostics() throws Exception {
    byte[] base = Files.readAllBytes(s003Path());
    var cases = List.of(
        c("zip-order", b -> reorder(b), true, "CPM_NON_CANONICAL"),
        c("zip-stored", b -> rewrite(b, "config.json", bytes -> bytes, ZipEntry.STORED), true, "CPM_NON_CANONICAL"),
        c("zip-time", b -> rewrite(b, "config.json", bytes -> bytes, ZipEntry.DEFLATED, 1L), true, "CPM_NON_CANONICAL"),
        c("config-utf8", b -> replace(b, "config.json", new byte[] {(byte) 0xc3, 0x28}), false, "CPM_CONFIG_INVALID"),
        c("config-duplicate", b -> replaceText(b, "config.json", "\"elements\":", "\"version\":1,\"elements\":"), false, "CPM_CONFIG_INVALID"),
        c("config-trailing", b -> appendEntry(b, "config.json", "x"), false, "CPM_CONFIG_INVALID"),
        c("config-unknown", b -> replaceText(b, "config.json", "{\"elements\":", "{\"extra\":1,\"elements\":"), false, "CPM_FEATURE_UNSUPPORTED"),
        c("config-number", b -> replaceText(b, "config.json", "\"version\":1", "\"version\":1.0"), false, "CPM_CONFIG_INVALID"),
        c("config-order", b -> reorderJsonFields(b, "config.json"), true, "CPM_NON_CANONICAL"),
        c("registry-reserved", b -> replaceText(b, "config.json", "\"storeID\":1000", "\"storeID\":6"), false, "CPM_INVALID_STORE_ID"),
        c("registry-collision", b -> replaceText(b, "config.json", "\"storeID\":1000", "\"storeID\":1"), false, "CPM_INVALID_STORE_ID"),
        c("uv-box", b -> replaceText(b, "config.json", "\"u\":0", "\"u\":1000"), false, "UV_OUT_OF_BOUNDS"),
        c("uv-texture-size", b -> replaceText(b, "config.json", "\"textureSize\":1", "\"textureSize\":0"), false, "UV_INVALID"),
        c("skin-missing", b -> removeEntry(b, "skin.png"), false, "PNG_DIMENSION_MISMATCH"),
        c("png-crc", b -> mutateSkin(b, x -> setByte(x, 40, (byte) (x[40] ^ 1))), false, "PNG_INVALID"),
        c("png-interlace", b -> mutateSkin(b, x -> setByte(x, 28, (byte) 1)), false, "PNG_INVALID"),
        c("png-profile", b -> mutateSkin(b, x -> setByte(x, 25, (byte) 2)), false, "PNG_INVALID"),
        c("png-budget", b -> mutateSkin(b, x -> Arrays.copyOf(x, x.length - 1)), false, "PNG_INVALID"),
        c("png-zlib", b -> mutateSkin(b, x -> setByte(x, 45, (byte) (x[45] ^ 1))), false, "PNG_INVALID"),
        c("animation-invalid-json", b -> replace(b, "animations/v_standing_minimal.json", "{".getBytes(StandardCharsets.UTF_8)), false, "CPM_ANIMATION_INVALID"),
        c("animation-unknown", b -> replaceText(b, "animations/v_standing_minimal.json", "{\"additive\":true", "{\"unknown\":1,\"additive\":true"), false, "CPM_FEATURE_UNSUPPORTED"),
        c("animation-vector", b -> replaceText(b, "animations/v_standing_minimal.json", "\"x\":0", "\"x\":\"bad\""), false, "CPM_ANIMATION_INVALID"),
        c("animation-color", b -> replaceText(b, "animations/v_standing_minimal.json", "\"ffffff\"", "\"zzzzzz\""), false, "CPM_ANIMATION_INVALID"),
        c("animation-frame-limit", b -> appendFrame(b), false, "CPM_ANIMATION_INVALID"),
        c("animation-dangling", b -> replaceText(b, "animations/v_standing_minimal.json", "\"storeID\":1000", "\"storeID\":9999"), false, "CPM_DANGLING_ANIMATION_REF"),
        c("animation-order", b -> reorderJsonFields(b, "animations/v_standing_minimal.json"), true, "CPM_NON_CANONICAL"));
    assertTrue(cases.size() >= 25);
    var validator = new CpmArtifactValidator();
    for (var testCase : cases) {
      byte[] mutated = testCase.mutation().apply(base);
      assertNotEquals(hex(base), hex(mutated), testCase.id());
      var result = validator.validate(mutated);
      assertEquals(testCase.success(), result.success(), testCase.id() + " diagnostics=" + result.diagnostics().all());
      assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals(testCase.code())), testCase.id());
    }
  }

  @Test
  void canonicalizerChecksAnimationBytes() throws Exception {
    byte[] base = Files.readAllBytes(s003Path());
    var result = new CpmArtifactValidator().validate(reorderJsonFields(base, "animations/v_standing_minimal.json"));
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals("CPM_NON_CANONICAL")));
  }

  private static MutationCase c(String id, UnaryOperator<byte[]> mutation, boolean success, String code) { return new MutationCase(id, mutation, success, code); }
  private static Path s003Path() { Path p = Path.of("..", "spikes", "minimal-cpmproject", "artifacts", "M5.cpmproject"); if (Files.exists(p)) return p; return Path.of("spikes", "minimal-cpmproject", "artifacts", "M5.cpmproject"); }
  private static String hex(byte[] b) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)); }
  private static byte[] replaceText(byte[] zip, String entry, String from, String to) { return replace(zip, entry, new String(entryBytes(zip, entry), StandardCharsets.UTF_8).replaceFirst(java.util.regex.Pattern.quote(from), java.util.regex.Matcher.quoteReplacement(to)).getBytes(StandardCharsets.UTF_8)); }
  private static byte[] appendEntry(byte[] zip, String entry, String suffix) { return replace(zip, entry, concat(entryBytes(zip, entry), suffix.getBytes(StandardCharsets.UTF_8))); }
  private static byte[] appendFrame(byte[] zip) { return replaceText(zip, "animations/v_standing_minimal.json", "],\"interpolator\"", "},{\"components\":[]}],\"interpolator\""); }
  private static byte[] replace(byte[] zip, String target, byte[] data) { return rewrite(zip, target, x -> data, ZipEntry.DEFLATED); }
  private static byte[] rewrite(byte[] zip, String target, UnaryOperator<byte[]> mutator, int method) { return rewrite(zip, target, mutator, method, 0L); }
  private static byte[] rewrite(byte[] zip, String target, UnaryOperator<byte[]> mutator, int method, long time) {
    try { var out = new ByteArrayOutputStream(); try (var in = new ZipInputStream(new ByteArrayInputStream(zip)); var zout = new ZipOutputStream(out)) { ZipEntry e; while ((e = in.getNextEntry()) != null) { byte[] bytes = in.readAllBytes(); if (e.getName().equals(target)) bytes = mutator.apply(bytes); var n = new ZipEntry(e.getName()); n.setMethod(method); if (method == ZipEntry.STORED) { n.setSize(bytes.length); n.setCompressedSize(bytes.length); n.setCrc(crc(bytes)); } if (time != 0) n.setTime(time); zout.putNextEntry(n); zout.write(bytes); zout.closeEntry(); } } return out.toByteArray(); } catch (IOException ex) { throw new UncheckedIOException(ex); }
  }
  private static long crc(byte[] bytes) { var c = new CRC32(); c.update(bytes); return c.getValue(); }
  private static byte[] removeEntry(byte[] zip, String target) { return rewrite(zip, target, x -> x, ZipEntry.DEFLATED).length == 0 ? zip : rewriteFiltered(zip, target); }
  private static byte[] rewriteFiltered(byte[] zip, String target) { try { var out = new ByteArrayOutputStream(); try (var in = new ZipInputStream(new ByteArrayInputStream(zip)); var zout = new ZipOutputStream(out)) { ZipEntry e; while ((e = in.getNextEntry()) != null) { byte[] bytes = in.readAllBytes(); if (e.getName().equals(target)) continue; zout.putNextEntry(new ZipEntry(e.getName())); zout.write(bytes); zout.closeEntry(); } } return out.toByteArray(); } catch (IOException ex) { throw new UncheckedIOException(ex); } }
  private static byte[] reorder(byte[] zip) { try { var out = new ByteArrayOutputStream(); var entries = new ArrayList<byte[]>(); var names = new ArrayList<String>(); try (var in = new ZipInputStream(new ByteArrayInputStream(zip))) { ZipEntry e; while ((e = in.getNextEntry()) != null) { names.add(e.getName()); entries.add(in.readAllBytes()); } } try (var z = new ZipOutputStream(out)) { for (int i = entries.size() - 1; i >= 0; i--) { z.putNextEntry(new ZipEntry(names.get(i))); z.write(entries.get(i)); z.closeEntry(); } } return out.toByteArray(); } catch (IOException ex) { throw new UncheckedIOException(ex); } }
  private static byte[] reorderJsonFields(byte[] zip, String entry) { try { var mapper = new ObjectMapper(); var node = mapper.readTree(entryBytes(zip, entry)); var out = mapper.createObjectNode(); var fields = node.fieldNames(); var names = new ArrayList<String>(); fields.forEachRemaining(names::add); if (names.size() < 2) return zip; for (int i = 1; i < names.size(); i++) out.set(names.get(i), node.get(names.get(i))); out.set(names.get(0), node.get(names.get(0))); return replace(zip, entry, (mapper.writeValueAsString(out) + "\n").getBytes(StandardCharsets.UTF_8)); } catch (Exception ex) { throw new IllegalStateException(ex); } }
  private static byte[] corruptLastByte(byte[] zip) { byte[] copy = zip.clone(); copy[copy.length - 1] ^= 1; return copy; }
  private static byte[] mutateSkin(byte[] zip, UnaryOperator<byte[]> mutation) { return replace(zip, "skin.png", mutation.apply(entryBytes(zip, "skin.png"))); }
  private static byte[] setByte(byte[] bytes, int index, byte value) { byte[] copy = bytes.clone(); if (index < copy.length) copy[index] = value; return copy; }
  private static byte[] entryBytes(byte[] zip, String target) { try (var in = new ZipInputStream(new ByteArrayInputStream(zip))) { ZipEntry e; while ((e = in.getNextEntry()) != null) { byte[] b = in.readAllBytes(); if (e.getName().equals(target)) return b; } return new byte[0]; } catch (IOException ex) { throw new UncheckedIOException(ex); } }
  private static byte[] concat(byte[] a, byte[] b) { byte[] r = Arrays.copyOf(a, a.length + b.length); System.arraycopy(b, 0, r, a.length, b.length); return r; }
}
