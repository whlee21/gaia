package gaia.bigdata.hbase.users;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.bigdata.api.user.User;
import gaia.bigdata.hbase.HBaseTable;
import gaia.bigdata.hbase.SuffixKeyValueSerializer;
import gaia.bigdata.hbase.ValueSerializer;

public class UserTable extends HBaseTable {
	private static transient Logger log = LoggerFactory.getLogger(UserTable.class);

	public static final byte[] TABLE = Bytes.toBytes("users");

	public static final byte[] INFO_CF = Bytes.toBytes("info");
	public static final byte[] ROLES_CF = Bytes.toBytes("roles");
	public static final String USERNAME = "username";
	public static final byte[] INFO_USERNAME_CQ = Bytes.toBytes("username");
	public static final byte[] INFO_PASSWORD_CQ = Bytes.toBytes("password");
	public static final byte[] INFO_VERSION_CQ = Bytes.toBytes("version");

	private static final byte[] NULL_BYTE = { 0 };

	private static final ValueSerializer<UserKey> keySerializer = new UserKeySerializer();
	private static final SuffixKeyValueSerializer serializer = new SuffixKeyValueSerializer();

	public UserTable(String zkConnect) {
		super(zkConnect);
	}

	public UserTable(HTablePool pool, Configuration conf) {
		super(pool, conf);
	}

	protected HTableDescriptor getTableDescriptor() {
		HTableDescriptor tDesc = new HTableDescriptor(TABLE);
		tDesc.addFamily(new HColumnDescriptor(INFO_CF));
		tDesc.addFamily(new HColumnDescriptor(ROLES_CF));
		return tDesc;
	}

	public void putUser(User user) throws IOException {
		HTableInterface table = pool.getTable(TABLE);
		Put put = newPut(user);
		log.debug("Put: {}", put);
		boolean ok;
		if (user.version == 0L)
			ok = table.checkAndPut(put.getRow(), INFO_CF, INFO_VERSION_CQ, null, put);
		else {
			ok = table.checkAndPut(put.getRow(), INFO_CF, INFO_VERSION_CQ, Bytes.toBytes(user.version), put);
		}
		if (!ok) {
			throw new ConcurrentModificationException("Version mismatch in user record - HBase was not updated for user: "
					+ user.username + " with version: " + user.version);
		}

		table.close();
	}

	public User getUser(String username) throws IOException {
		Get get = newGet(username);
		log.debug("Get: {}", get);
		HTableInterface table = pool.getTable(TABLE);
		Result result = table.get(get);
		table.close();
		return resultToUser(result);
	}

	public void deleteUser(String username) throws IOException {
		UserKey key = new UserKey(username);
		byte[] row = keySerializer.toBytes(key);
		Delete delete = new Delete(row);
		log.debug("Delete: {}", delete);
		HTableInterface table = pool.getTable(TABLE);
		table.delete(delete);
		table.close();
	}

	public User updateUser(User user) throws IOException {
		User actual = getUser(user.username);
		if (user.password != null) {
			actual.password = user.password;
		}
		if (user.properties != null) {
			actual.properties = user.properties;
		}
		if (user.roles != null) {
			actual.roles = user.roles;
		}
		putUser(actual);
		return actual;
	}

	public static Put newPut(User user) throws IOException {
		Put put = new Put(keySerializer.toBytes(new UserKey(user.username)));

		put.add(INFO_CF, INFO_USERNAME_CQ, Bytes.toBytes(user.username));

		put.add(INFO_CF, INFO_VERSION_CQ, Bytes.toBytes(user.version + 1L));

		if (user.password != null) {
			put.add(INFO_CF, INFO_PASSWORD_CQ, Bytes.toBytes(user.password));
		}

		if (user.properties != null) {
			for (Map.Entry<String, String> prop : user.properties.entrySet()) {
				SuffixKeyValueSerializer.KeyValue<byte[], byte[]> kv = serializer.toBytes((String) prop.getKey(),
						prop.getValue());
				put.add(INFO_CF, (byte[]) kv.key, (byte[]) kv.value);
			}

		}

		if (user.roles != null) {
			for (String role : user.roles) {
				put.add(ROLES_CF, Bytes.toBytes(role), NULL_BYTE);
			}
		}

		return put;
	}

	public static Get newGet(String username) throws IOException {
		Get get = new Get(keySerializer.toBytes(new UserKey(username)));

		get.addColumn(INFO_CF, INFO_USERNAME_CQ);
		get.addColumn(INFO_CF, INFO_PASSWORD_CQ);
		get.addColumn(INFO_CF, INFO_VERSION_CQ);

		get.addFamily(INFO_CF);

		get.addFamily(ROLES_CF);

		return get;
	}

	protected User resultToUser(Result result) throws IOException {
		if (result.isEmpty()) {
			return null;
		}
		long version = 0L;
		String password = null;

		Map<byte[], byte[]> info = result.getFamilyMap(INFO_CF);
		UserKey key = (UserKey) keySerializer.toObject(result.getRow());

		Map<String, String> userInfo = new HashMap<String, String>(info.size());
		for (Map.Entry<byte[], byte[]> entry : info.entrySet()) {
			if (!Bytes.equals((byte[]) entry.getKey(), INFO_USERNAME_CQ)) {
				if (Bytes.equals((byte[]) entry.getKey(), INFO_PASSWORD_CQ)) {
					password = Bytes.toString((byte[]) entry.getValue());
				} else if (Bytes.equals((byte[]) entry.getKey(), INFO_VERSION_CQ)) {
					version = Bytes.toLong((byte[]) entry.getValue());
				} else {
					SuffixKeyValueSerializer.KeyValue<String, Object> kv = serializer.toObject((byte[]) entry.getKey(),
							(byte[]) entry.getValue());
					userInfo.put(kv.key, (String) kv.value);
				}
			}
		}
		Set<String> roles = new HashSet<String>();
		for (byte[] roleBytes : result.getFamilyMap(ROLES_CF).keySet()) {
			roles.add(Bytes.toString(roleBytes));
		}

		User user = new User(key.username, password, version);
		user.properties = userInfo;
		user.roles = roles;
		return user;
	}

	public Iterable<User> listUsers() throws IOException {
		Scan scan = new Scan();
		scan.addFamily(INFO_CF);
		scan.addFamily(ROLES_CF);
		return wrapResultIterable(scan);
	}

	public Iterable<User> grepUser(byte[] column, String pattern) throws IOException {
		Scan scan = new Scan();
		scan.addFamily(INFO_CF);
		SingleColumnValueFilter filter = new SingleColumnValueFilter(INFO_CF, column, CompareFilter.CompareOp.EQUAL,
				new RegexStringComparator(pattern));

		filter.setFilterIfMissing(true);
		filter.setLatestVersionOnly(true);
		scan.setFilter(filter);
		return wrapResultIterable(scan);
	}

	public Iterable<User> grepUser(String pattern) throws IOException {
		return grepUser(INFO_USERNAME_CQ, pattern);
	}

	public Iterable<User> listUsersHavingRole(String role) throws IOException {
		Scan scan = new Scan();
		scan.addFamily(INFO_CF);
		scan.addFamily(ROLES_CF);
		scan.setFilter(new QualifierFilter(CompareFilter.CompareOp.EQUAL, new BinaryComparator(Bytes.toBytes(role))));
		return wrapResultIterable(scan);
	}

	public void deleteRoleFromUser(String username, String role) throws IOException {
		UserKey key = new UserKey(username);
		byte[] row = keySerializer.toBytes(key);
		Delete delete = new Delete(row);
		delete.deleteColumn(ROLES_CF, Bytes.toBytes(role));
		log.debug("Delete: {}", delete);
		HTableInterface table = pool.getTable(TABLE);
		table.delete(delete);
		table.close();
	}

	private Iterable<User> wrapResultIterable(final Scan scan) {
		return new Iterable<User>() {
			public Iterator<User> iterator() {
				HTableInterface table = pool.getTable(UserTable.TABLE);
				try {
					final Iterator<Result> results = table.getScanner(scan).iterator();
					return new Iterator<User>() {
						public boolean hasNext() {
							return results.hasNext();
						}

						public User next() {
							Result result = (Result) results.next();
							try {
								return resultToUser(result);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}

						public void remove() {
							throw new UnsupportedOperationException();
						}
					};
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					try {
						table.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		};
	}

	protected byte[][] getSplits() {
		return new byte[0][];
	}
}
