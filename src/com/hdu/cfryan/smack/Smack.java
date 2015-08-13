package com.hdu.cfryan.smack;

import org.jivesoftware.smack.packet.Message;

import com.hdu.cfryan.exception.XXException;

/**
 * ���ȶ���һЩ�ӿڣ���Ҫʵ��һЩʲô���Ĺ��ܣ�
 * 
 * @author way
 * 
 */
public interface Smack {
	/**
	 * ��½
	 * 
	 * @param account
	 *            �˺�
	 * @param password
	 *            ����
	 * @return �Ƿ��½�ɹ�
	 * @throws XXException
	 *             �׳��Զ����쳣���Ա�ͳһ�����½ʧ�ܵ�����
	 */
	public boolean login(String account, String password) throws XXException;

	/**
	 * ע����½
	 * 
	 * @return �Ƿ�ɹ�
	 */
	public boolean logout();

	/**
	 * �Ƿ��Ѿ������Ϸ�����
	 * 
	 * @return
	 */
	public boolean isAuthenticated();

	/**
	 * ��Ӻ���
	 * 
	 * @param user
	 *            ����id
	 * @param alias
	 *            �ǳ�
	 * @param group
	 *            ���ڵķ���
	 * @throws XXException
	 */
	public void addRosterItem(String user, String alias, String group)
			throws XXException;

	/**
	 * ɾ������
	 * 
	 * @param user
	 *            ����id
	 * @throws XXException
	 */
	public void removeRosterItem(String user) throws XXException;

	/**
	 * �޸ĺ����ǳ�
	 * 
	 * @param user
	 *            ����id
	 * @param newName
	 *            ���ǳ�
	 * @throws XXException
	 */
	public void renameRosterItem(String user, String newName)
			throws XXException;


	/**
	 * �������������Ȩ��������Ӻ���ʧ��ʱ���ظ���� �ٴ���Է���������
	 * 
	 * @param user
	 *            ����id
	 */
	public void requestAuthorizationForRosterItem(String user);


	/**
	 * ���õ�ǰ����״̬
	 */
	public void setStatusFromConfig();

	/**
	 * ������Ϣ
	 * 
	 * @param user
	 * @param message
	 */
	public void sendMessage(Message message, int ds, Boolean compress);

	/**
	 * ����������������������ֳ����� ͨ��һ�����ӿ��ƣ���ʱ���ͣ�
	 */
	public void sendServerPing();

	/**
	 * ��jid�л�ȡ������
	 * 
	 * @param jid
	 * @return
	 */
	public String getNameForJID(String jid);
}
