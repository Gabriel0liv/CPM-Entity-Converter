package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedStoreReferenceValidator {
  DiagnosticBag validate(CpmPersistedProjectV1 project) {
    // The MVP has no independent static references. The registry itself is
    // validated while materializing the project; animation references are
    // validated by CpmPersistedAnimationValidator.
    return new DiagnosticBag();
  }
}
