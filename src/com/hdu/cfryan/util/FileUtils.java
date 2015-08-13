package com.hdu.cfryan.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	private String SDROOT;
	
	private int BUFFSIZE = 4 * 1024; 
	
	public String getSDPATH(){
		return SDROOT;
	}
	
	public FileUtils(){
		//�õ���ǰ�ⲿ�洢�豸��Ŀ¼( /SDCARD )
		SDROOT = Environment.getExternalStorageDirectory() + "/";
	}
	
	/**
	 * ��SD���ϴ����ļ�
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public File createSDFile(String fileName) throws IOException{
		File file = new File(SDROOT + fileName);
		Log.i("createSDFile",SDROOT + fileName);
		file.createNewFile();
		return file;
	}
	
	/**
	 * �������Ŀ¼�������򴴽�Ŀ¼
	 * 
	 * @param destDirName
	 * @return
	 */
	public boolean createSDDir(String destDirName)
	{
		File dir = new File(destDirName);
		if (dir.exists())
		{
			System.out.println("����Ŀ¼" + destDirName + "ʧ�ܣ�Ŀ��Ŀ¼�Ѿ�����");
			return false;
		}
		if (!destDirName.endsWith(File.separator))
		{
			destDirName = destDirName + File.separator;
		}
		// ����Ŀ¼
		if (dir.mkdirs())
		{
			System.out.println("����Ŀ¼" + destDirName + "�ɹ���");
			return true;
		} else
		{
			System.out.println("����Ŀ¼" + destDirName + "ʧ�ܣ�");
			return false;
		}
	}
	
	/**
	 * �ж�SD���ϵ��ļ����Ƿ����
	 * @param fileName
	 * @return
	 */
	public boolean isFileExist(String fileName){
		File file = new File(SDROOT + fileName);
		return file.exists();
	}
	
	/**
	 * ��һ��InputStream���������д�뵽SD����
	 * @param dirName
	 * @param fileName
	 * @param is
	 * @return
	 */
	public File writeToSDFromInputStream(String dirName,String fileName, InputStream is){
		
		File file = null;
		OutputStream os = null;
		try {
			file = createSDFile(dirName + fileName);
			os = new FileOutputStream(file);
            
			byte[] buffer = new byte[BUFFSIZE];
            while((is.read(buffer)) > 0){
				os.write(buffer);
			}

			os.flush();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
	

	/**
	 * ��һ��Bitmap���������д�뵽SD����
	 * @param dirName
	 * @param fileName
	 * @param bmp
	 * @return
	 */
	public void writeToSDFromBmp(String dirName, String fileName, Bitmap bmp) {

		try {
		//	Log.i("writeToSDFromBmp",dirName);
		//	createSDDir(dirName);
			File file = createSDFile(dirName + fileName);
			compressBmpToFile(bmp, file, false);
		} catch (FileNotFoundException e) {
			Log.e("writeToSDFromBmp", "FileNotFoundException failed");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("writeToSDFromBmp", "IOException failed");
			e.printStackTrace();
		}
		
	}
	
	public void compressBmpToFile(Bitmap bmp, File file, boolean compress) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int options;
		if(compress){
			options = 80;
			bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
			while (baos.toByteArray().length / 1024 > 100) {
				baos.reset();
				options -= 10;
				bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
			}
		}else{
			bmp.compress(Bitmap.CompressFormat.JPEG, 1, baos);
		}
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baos.toByteArray());
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
