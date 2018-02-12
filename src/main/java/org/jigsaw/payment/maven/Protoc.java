package org.jigsaw.payment.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Protoc {
	private static final Logger log = LoggerFactory.getLogger(Protoc.class);

	private static String[] sStdTypesProto2 = { "include/google/protobuf/descriptor.proto", };
	private static String[] sStdTypesProto3 = { "include/google/protobuf/any.proto",
			"include/google/protobuf/api.proto", "include/google/protobuf/descriptor.proto",
			"include/google/protobuf/duration.proto", "include/google/protobuf/empty.proto",
			"include/google/protobuf/field_mask.proto", "include/google/protobuf/source_context.proto",
			"include/google/protobuf/struct.proto", "include/google/protobuf/timestamp.proto",
			"include/google/protobuf/type.proto", "include/google/protobuf/wrappers.proto", };

	static Map<String, String[]> sStdTypesMap = new HashMap<String, String[]>();
	static {
		sStdTypesMap.put("2", sStdTypesProto2);
		sStdTypesMap.put("3", sStdTypesProto3);
	}

	public int runProtoc(String[] args) throws IOException, InterruptedException {
		ProtocVersion protocVersion = ProtocVersion.PROTOC_VERSION;
		boolean includeStdTypes = false;
		for (String arg : args) {
			ProtocVersion v = getVersion(arg);
			if (v != null)
				protocVersion = v;
			if (arg.equals("--include_std_types"))
				includeStdTypes = true;
		}

		try {
			File protocTemp = extractProtoc(protocVersion, includeStdTypes, null);
			return runProtoc(protocTemp.getAbsolutePath(), args);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (Exception e) {
			// some linuxes don't allow exec in /tmp, try user home
			String homeDir = System.getProperty("user.home");
			File protocTemp = extractProtoc(protocVersion, includeStdTypes, new File(homeDir));
			return runProtoc(protocTemp.getAbsolutePath(), args);
		}
	}

	private int runProtoc(String cmd, String[] args) throws IOException, InterruptedException {
		List<String> argList = Arrays.asList(args);

		List<String> protocCmd = new ArrayList<String>();
		protocCmd.add(cmd);
		for (String arg : argList) {
			if (arg.equals("--include_std_types")) {
				File stdTypeDir = new File(new File(cmd).getParentFile().getParentFile(), "include");
				protocCmd.add("-I" + stdTypeDir.getAbsolutePath());
			} else {
				ProtocVersion v = getVersion(arg);
				if (v == null)
					protocCmd.add(arg);
			}
		}

		Process protoc = null;
		int numTries = 1;
		while (protoc == null) {
			try {
				log.info("executing: " + protocCmd);
				ProcessBuilder pb = new ProcessBuilder(protocCmd);
				protoc = pb.start();
			} catch (IOException ioe) {
				if (numTries++ >= 3)
					throw ioe; // retry loop, workaround text file busy issue
				log.error("caught exception, retrying: " + ioe);
				Thread.sleep(1000);
			}
		}

		new Thread(new StreamCopier(protoc.getInputStream(), System.out)).start();
		new Thread(new StreamCopier(protoc.getErrorStream(), System.err)).start();
		int exitCode = protoc.waitFor();

		return exitCode;
	}

	private File extractProtoc(ProtocVersion protocVersion, boolean includeStdTypes, File dir) throws IOException {
		File protocTemp = extractProtoc(protocVersion, dir);
		if (includeStdTypes)
			extractStdTypes(protocVersion, protocTemp.getParentFile().getParentFile());
		return protocTemp;
	}

	private File extractProtoc(ProtocVersion protocVersion, File dir) throws IOException {
		log.info("protoc version: " + protocVersion);

		File tmpDir = File.createTempFile("jigsawrpc", "", dir);
		tmpDir.delete();
		tmpDir.mkdirs();
		tmpDir.deleteOnExit();
		File binDir = new File(tmpDir, "bin");
		binDir.mkdirs();
		binDir.deleteOnExit();

		File protocTemp = new File(binDir, "protoc");

		populateFile("bin/protoc", protocTemp);
		protocTemp.setExecutable(true);
		protocTemp.deleteOnExit();
		return protocTemp;
	}

	private File extractStdTypes(ProtocVersion protocVersion, File tmpDir) throws IOException {
		if (tmpDir == null) {
			tmpDir = File.createTempFile("jigsawrpc", "");
			tmpDir.delete();
			tmpDir.mkdirs();
			tmpDir.deleteOnExit();
		}

		File tmpDirProtos = new File(tmpDir, "include/google/protobuf");
		tmpDirProtos.mkdirs();
		tmpDirProtos.getParentFile().getParentFile().deleteOnExit();
		tmpDirProtos.getParentFile().deleteOnExit();
		tmpDirProtos.deleteOnExit();

		final String majorProtoVersion = String.valueOf(protocVersion.mVersion.charAt(0));
		final String srcPathPrefix = String.format("proto%s/", majorProtoVersion);
		final String[] stdTypes = sStdTypesMap.get(majorProtoVersion);
		for (String srcFilePath : stdTypes) {
			File tmpFile = new File(tmpDir, srcFilePath);
			populateFile(srcPathPrefix + srcFilePath, tmpFile);
			tmpFile.deleteOnExit();
		}

		return tmpDir;
	}

	private File populateFile(String srcFilePath, File destFile) throws IOException {
		String resourcePath = "/" + srcFilePath;
		InputStream is = Protoc.class.getResourceAsStream(resourcePath);

		try {

			FileUtils.copyInputStreamToFile(is, destFile);
		} finally {
			if (is != null)
				is.close();

		}

		return destFile;
	}

	private ProtocVersion getVersion(String spec) {
		return ProtocVersion.getVersion(spec);
	}

	private class StreamCopier implements Runnable {

		private InputStream mIn;
		private OutputStream mOut;

		public StreamCopier(InputStream in, OutputStream out) {
			mIn = in;
			mOut = out;
		}

		public void run() {
			try {
				IOUtils.copy(mIn, mOut);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}

	}

	public static void main(String[] args) {
		try {
			if (args.length > 0 && args[0].equals("-pp")) { // print platform
				PlatformDetector.main(args);
				return;
			}
			Protoc protoc = new Protoc();
			int exitCode = protoc.runProtoc(args);
			System.exit(exitCode);
		} catch (Exception e) {
			log.error("Error in running jigsaw-protoc-plugin", e);
		}
	}
}
