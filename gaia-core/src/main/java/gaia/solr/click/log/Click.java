package gaia.solr.click.log;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

public class Click implements Writable {
	private Text id = new Text();
	private int position;
	private long time;

	public Click() {
	}

	public Click(Text docId, int position, long time) {
		id = docId;
		this.position = position;
		this.time = time;
	}

	public Text getId() {
		return id;
	}

	public void setId(String id) {
		this.id.set(id);
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + (id == null ? 0 : id.hashCode());
		result = prime * result + position;
		result = prime * result + (int) (time ^ time >>> 32);
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Click other = (Click) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (position != other.position)
			return false;
		if (time != other.time)
			return false;
		return true;
	}

	public String toString() {
		return "id=" + id + ",time=" + time + ",pos=" + position;
	}

	public void readFields(DataInput in) throws IOException {
		id.readFields(in);
		position = WritableUtils.readVInt(in);
		time = WritableUtils.readVLong(in);
	}

	public void write(DataOutput out) throws IOException {
		id.write(out);
		WritableUtils.writeVInt(out, position);
		WritableUtils.writeVLong(out, time);
	}
}
