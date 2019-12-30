package com.example.myapplication;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PositionalDataSource;

import java.util.ArrayList;
import java.util.List;

public class ContactsViewModel extends ViewModel {

    private ContentResolver contentResolver;
    public LiveData<PagedList<Model>> contactsList;

    public ContactsViewModel(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public void loadContacts() {
        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(100)
                .setPageSize(100)
                .setEnablePlaceholders(false)
                .build();

        contactsList = new LivePagedListBuilder<Integer, Model>(
                new ContactsDataSourceFactory(contentResolver), config).build();
    }


    public class ContactsDataSourceFactory extends DataSource.Factory<Integer, Model> {

        private ContentResolver contentResolver;

        public ContactsDataSourceFactory(ContentResolver contentResolver) {
            this.contentResolver = contentResolver;
        }

        @NonNull
        @Override
        public DataSource<Integer, Model> create() {
            return new ContactsDataSource(contentResolver);
        }
    }

    public class ContactsDataSource extends PositionalDataSource<Model> {
        private ContentResolver contentResolver;

        public ContactsDataSource(ContentResolver contentResolver) {
            this.contentResolver = contentResolver;
        }

        @Override
        public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Model> callback) {
            callback.onResult(getContacts(params.requestedLoadSize,params.requestedStartPosition),0);
        }

        @Override
        public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Model> callback) {
            callback.onResult(getContacts(params.loadSize,params.startPosition));
        }


        List<Model> getContacts(int limit, int offset){
            List<Model> list = new ArrayList<>();
            Cursor cur = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY +
                            " ASC LIMIT " + limit + " OFFSET " + offset);

            if ((cur != null ? cur.getCount() : 0) > 0) {
                while (cur != null && cur.moveToNext()) {
                    String id = cur.getString(
                            cur.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cur.getString(cur.getColumnIndex(
                            ContactsContract.Contacts.DISPLAY_NAME));

                    if (cur.getInt(cur.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null);
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));

                            Log.i("aminContact", "Name: " + name);
                            Log.i("aminContact", "Phone Number: " + phoneNo);

                            Model model = new Model();
                            model.setName(name);
                            model.setPhone(phoneNo);

                            list.add(model);
                        }
                        pCur.close();

                    }
                }
                Log.i("aminContact", "contact Size: " + list.size() +"");
                return list;
            }
            if(cur!=null){
                cur.close();
            }
            return null;
        }
    }
}
