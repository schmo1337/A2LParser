package net.alenzen.a2l;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.alenzen.a2l.Asap2FileTest.TestFile;

class MatrixDimTest {
	private Asap2File file;
	private MatrixDim matrixDim;
	
	@BeforeEach
	void initTestfile() throws IOException {
		file = Asap2FileTest.getTestFile(TestFile.A);
		matrixDim = file.getProject().getModules().get(0).getMeasurements().get(0).getMatrixDim();

		assertNotNull(matrixDim);
	}
	@Test
	void testAllDimensions() {
		assertEquals(7L, matrixDim.getxDim());
		assertEquals(8L, matrixDim.getyDim());
		assertEquals(10L, matrixDim.getzDim());
	}

}
