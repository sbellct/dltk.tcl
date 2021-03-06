package org.eclipse.dltk.tcl.parser.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.tcl.parser.ITclErrorConstants;
import org.eclipse.dltk.tcl.parser.ITclParserOptions;
import org.eclipse.dltk.tcl.parser.TclError;
import org.eclipse.dltk.tcl.parser.TclErrorCollector;
import org.eclipse.dltk.tcl.parser.TclParser;
import org.eclipse.dltk.tcl.parser.definitions.DefinitionManager;
import org.eclipse.dltk.tcl.parser.definitions.NamespaceScopeProcessor;
import org.junit.Test;

public class ErrorReportingTests {
	NamespaceScopeProcessor processor = new NamespaceScopeProcessor();

	@Test
	public void test001() {
		String source = "namespace eval";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test002() {
		String source = "eval";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test003() {
		String source = "fcopy inchan";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test004() {
		String source = "fcopy inchan outchan -size";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test005() {
		String source = "fcopy inchan outchan -size -command callback";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.INVALID_ARGUMENT_VALUE);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test006() {
		String source = "namespace eval nms {puts 3}";
		List<Integer> errorCodes = new ArrayList<>();
		typedCheck(source, errorCodes);
	}

	@Test
	public void test007() {
		String source = "switch alpha c1 history c2 history c3";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.EXTRA_ARGUMENTS);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test008() {
		String source = "namespace";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test009() {
		String source = "foreach";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test010() {
		String source = "read $in";
		List<Integer> errorCodes = new ArrayList<>();
		typedCheck(source, errorCodes);
	}

	@Test
	public void test011() {
		String source = "string is . .";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.INVALID_ARGUMENT_VALUE);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test012() {
		String source = "fcopy inchan outchan -size -size 34";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.EXTRA_ARGUMENTS);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test013() {
		String source = "subst -nocommands -novariables $val";
		List<Integer> errorCodes = new ArrayList<>();
		typedCheck(source, errorCodes);
	}

	@Test
	public void test014() {
		String source = "after";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test015() {
		String source = "linsert list";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test016() {
		String source = "foreach";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test017() {
		String source = "after c";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test018() {
		String source = "after k";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.INVALID_ARGUMENT_VALUE);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test019() {
		String source = "registry set $key $value $data $mod";
		List<Integer> errorCodes = new ArrayList<>();
		typedCheck(source, errorCodes);
	}

	@Test
	public void test020() {
		String source = "linsert list f";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.INVALID_ARGUMENT_VALUE);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test021() {
		String source = "fconfigure stdin";
		List<Integer> errorCodes = new ArrayList<>();
		typedCheck(source, errorCodes);
	}

	@Test
	public void test022() {
		String source = "socket $host";
		List<Integer> errorCodes = new ArrayList<>();
		errorCodes.add(ITclErrorConstants.MISSING_ARGUMENT);
		typedCheck(source, errorCodes);
	}

	@Test
	public void test023() {
		String source = "switch a {#(v {puts lala}}";
		List<Integer> errorCodes = new ArrayList<>();
		typedCheck(source, errorCodes);
	}

	// public void test021() {
	// String source = "proc {{arg def1 def2}} {puts $arg}";
	// List<Integer> errorCodes = new ArrayList<Integer>();
	// errorCodes.add(ITclErrorReporter.INVALID_ARGUMENT_VALUE);
	// typedCheck(source, errorCodes);
	// }

	// public void test022() {
	// String source = "proc {{arg def1 def2}} {{arg def1 def2}}";
	// List<Integer> errorCodes = new ArrayList<Integer>();
	// errorCodes.add(ITclErrorReporter.INVALID_ARGUMENT_VALUE);
	// typedCheck(source, errorCodes);
	// }

	private void typedCheck(String source, List<Integer> errorCodes) {
		processor = DefinitionManager.getInstance().createProcessor();
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement element = stackTrace[2];
		System.out.println("%%%%%%%%%%%%%%%%Test:" + element.getMethodName());
		TclParser parser = TestUtils.createParser("8.4");
		TclErrorCollector errors = new TclErrorCollector();
		parser.setOptionValue(ITclParserOptions.REPORT_UNKNOWN_AS_ERROR, true);
		parser.parse(source, errors, processor);
		if (errors.getCount() > 0) {
			TestUtils.outErrors(source, errors);
		}
		assertEquals(errorCodes.size(), errors.getCount());
		int count = 0;
		for (int code : errorCodes) {
			for (TclError tclError : errors.getErrors()) {
				if (tclError.getCode() == code) {
					count++;
					break;
				}
			}
		}
		assertEquals(errorCodes.size(), count);
	}
}
