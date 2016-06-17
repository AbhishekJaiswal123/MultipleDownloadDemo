package com.abhishek.multipledownloads;

import android.os.Parcel;
import android.os.Parcelable;



public class File1 implements Parcelable {
	private final long id;
	private final String url;
	public int progress;

	public File1(long id, String url) {
		this.id = id;
		this.url = url;
	}

	public long getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}
	
	public int getProgress(){
		return progress;
	}

	@Override
	public String toString() {
		return url;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(this.id);
		dest.writeString(this.url);
		dest.writeInt(this.progress);
	}

	private File1(Parcel in) {
		this.id = in.readLong();
		this.url = in.readString();
		this.progress = in.readInt();
	}

	public static final Parcelable.Creator<File1> CREATOR = new Parcelable.Creator<File1>() {
		public File1 createFromParcel(Parcel source) {
			return new File1(source);
		}

		public File1[] newArray(int size) {
			return new File1[size];
		}
	};
}
