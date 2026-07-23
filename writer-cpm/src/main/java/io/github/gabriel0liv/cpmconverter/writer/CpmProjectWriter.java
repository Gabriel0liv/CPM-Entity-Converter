package io.github.gabriel0liv.cpmconverter.writer;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.projection.*;
import java.util.*;

public final class CpmProjectWriter {
  public Result<CpmProjectArtifact> write(CpmProjectWriteRequest request) {
    if (request == null || request.projection() == null || request.skinPng() == null || request.skinPng().length == 0)
      return Result.failure(error("request", "request and non-empty skin PNG are required"));
    var identified = request.projection();
    var validation = new CpmLogicalProjectionValidator().validate(identified.logicalProjection());
    validation = validation.addAll(new CpmStoreIdAssignmentValidator().validate(identified));
    for (var root : identified.logicalProjection().project().roots()) {
      var rootId = identified.storeIds().findByNode(new CpmNodeKey("root:" + root.root().id()));
      if (rootId.isEmpty()) validation = validation.add(error("root", "reserved root assignment is missing"));
      for (var element : root.children()) validation = checkElements(element, identified, validation);
    }
    if (validation.hasErrors()) return Result.failure(validation);
    try {
      var config = new CpmConfigJsonWriter().write(identified);
      var bytes = new CpmDeterministicZipWriter().write(config, request.skinPng());
      return Result.success(CpmProjectArtifact.of(bytes));
    } catch (java.io.IOException e) {
      return Result.failure(error("zip", e.getMessage() == null ? "write failed" : e.getMessage()));
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return Result.failure(error("projection", "projection cannot be serialized"));
    }
  }
  private static DiagnosticBag checkElements(CpmLogicalElementV1 element, CpmIdentifiedProjectionV1 identified, DiagnosticBag bag) {
    if (identified.storeIds().findByNode(element.key()).isEmpty()) bag = bag.add(error("storeID", "element assignment is missing"));
    for (var child : element.children()) bag = checkElements(child, identified, bag);
    return bag;
  }
  private static Diagnostic error(String field, String message) {
    return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_WRITE_FAILED), null, message, "Repair the CPM projection or texture payload before writing.", null, null, new TreeMap<>(Map.of("field", field)));
  }
}
