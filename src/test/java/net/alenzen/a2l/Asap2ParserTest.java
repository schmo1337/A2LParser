package net.alenzen.a2l;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidParameterException;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import net.alenzen.a2l.Asap2FileTest.TestFile;

public class Asap2ParserTest {
	private final String TEST_ESCAPE_STRING = "\\" + "\"" + "'" + "\n" + "\r" + "\t";

	@Test
	void testToA2LString() {
		String result = A2LWriter.toA2LString(TEST_ESCAPE_STRING);
		assertEquals("\"\\\\\\\"\\'\\n\\r\\t\"", result);
	}

	@Test
	void testToA2LString_backAndForth() throws InvalidParameterException {
		String resultA = A2LWriter.toA2LString(TEST_ESCAPE_STRING);
		String resultB = A2LVisitor.toJavaString(resultA);
		assertEquals(TEST_ESCAPE_STRING, resultB);
	}

	@Test
	void testUTF8BOMOutput() throws JsonGenerationException, JsonMappingException, IOException, URISyntaxException {
		testBOMOutput(StandardCharsets.UTF_8);
	}

	@Test
	void testUTF16LEBOMOutput() throws JsonGenerationException, JsonMappingException, URISyntaxException, IOException {
		testBOMOutput(StandardCharsets.UTF_16LE);
	}

	@Test
	void testUTF16BEBOMOutput() throws JsonGenerationException, JsonMappingException, URISyntaxException, IOException {
		testBOMOutput(StandardCharsets.UTF_16BE);
	}

	@Test
	void testUTF32LEBOMOutput() throws JsonGenerationException, JsonMappingException, URISyntaxException, IOException {
		testBOMOutput(Charset.forName("UTF-32LE"));
	}

	@Test
	void testUTF32BEBOMOutput() throws JsonGenerationException, JsonMappingException, URISyntaxException, IOException {
		testBOMOutput(Charset.forName("UTF-32BE"));
	}

	@Test
	void testSystemOutResult() throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {
		URL testFileResource = ClassLoader.getSystemResource(TestFile.A.getFilename());
		String a2lPath = Paths.get(testFileResource.toURI()).toFile().getAbsolutePath();

		String[] arguments = new String[] { "-a2l", a2lPath };

		PrintStream stdOut = System.out;
		try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
			System.setOut(new PrintStream(outStream));

			Asap2Parser.main(arguments);

			String fileOutput = outStream.toString();

			assertEquals(Asap2FileTest.TEST_FILE_A_JSON, fileOutput);
		} finally {
			System.setOut(stdOut);
		}
	}

	@Test
	void testSystemInA2lFile() throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {
		try (InputStream a2lFileContent = ClassLoader.getSystemResourceAsStream(TestFile.A.getFilename())) {
			String[] arguments = new String[] { "-a2l" };

			PrintStream stdOut = System.out;
			InputStream stdIn = System.in;
			try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
				System.setOut(new PrintStream(outStream));
				System.setIn(a2lFileContent);

				Asap2Parser.main(arguments);

				String fileOutput = outStream.toString();

				assertEquals(Asap2FileTest.TEST_FILE_A_JSON, fileOutput);
			} finally {
				System.setOut(stdOut);
				System.setIn(stdIn);
			}
		}
	}

	@Test
	void testSystemInJsonFile() throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {
		try (InputStream a2lFileContent = new ByteArrayInputStream(Asap2FileTest.TEST_FILE_A_JSON.getBytes(StandardCharsets.UTF_8))) {
			String[] arguments = new String[] { "-j" };

			PrintStream stdOut = System.out;
			InputStream stdIn = System.in;
			try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
				System.setOut(new PrintStream(outStream));
				System.setIn(a2lFileContent);

				Asap2Parser.main(arguments);

				String fileOutput = outStream.toString();
				Asap2Parser parser = new Asap2Parser(new ByteArrayInputStream(fileOutput.getBytes()));
				parser.setEventHandler((a, b, c) -> {
					fail(a + ":" + b + ":" + c);
				});
				Asap2File fromOutput = parser.parse();

				assertEquals(Asap2FileTest.TEST_FILE_A_JSON, fromOutput.toJson());
			} finally {
				System.setOut(stdOut);
				System.setIn(stdIn);
			}
		}
	}

	private void testBOMOutput(Charset charsetToTest)
			throws URISyntaxException, IOException, JsonGenerationException, JsonMappingException {
		URL testFileResource = ClassLoader.getSystemResource(TestFile.A.getFilename());
		String a2lPath = Paths.get(testFileResource.toURI()).toFile().getAbsolutePath();
		String encoding = charsetToTest.displayName();
		File tempOutputFile = File.createTempFile(a2lPath, ".json");

		try {
			String tempFile = tempOutputFile.getAbsolutePath();
			String[] arguments = new String[] { "-a2l", a2lPath, "-o", tempFile, "-c", encoding };

			Asap2Parser.main(arguments);

			assertBOM(tempFile, charsetToTest);
		} finally {
			tempOutputFile.delete();
		}
	}

	private void assertBOM(String filePath, Charset charsetExpected) throws IOException {
		try (BOMInputStream bomIn = new BOMInputStream(new FileInputStream(filePath), ByteOrderMark.UTF_8,
				ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE)) {

			if (!bomIn.hasBOM()) {
				fail("File does not contain any known BOM!");
			}

			if (!bomIn.hasBOM(Asap2Parser.determineBOM(charsetExpected))) {
				fail("Expected BOM is " + charsetExpected.displayName() + " but instead is "
						+ bomIn.getBOMCharsetName());
			}
		}
	}
}