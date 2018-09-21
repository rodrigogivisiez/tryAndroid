package com.tullyapp.tully.Utils;

import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

public class ObscuredSharedPreferences implements SharedPreferences {
    private CryptoManager crypt;
    private SharedPreferences delegate;

    public ObscuredSharedPreferences(SharedPreferences delegate, String password) {
        this.delegate = delegate;
        this.crypt = new CryptoManager(password);
    }

    public Editor edit() {
        return new Editor();
    }

    @Override
    public Map<String, ?> getAll() {
        throw new UnsupportedOperationException("This method is not implemented in " + ObscuredSharedPreferences.class.getSimpleName()); // left as an exercise to the reader
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        final String v = delegate.getString(crypt.encrypt(key), null);
        return v != null ? Boolean.parseBoolean(crypt.decrypt(v)) : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        final String v = delegate.getString(crypt.encrypt(key), null);
        return v != null ? Float.parseFloat(crypt.decrypt(v)) : defValue;
    }

    @Override
    public int getInt(String key, int defValue) {
        final String v = delegate.getString(crypt.encrypt(key), null);
        return v != null ? Integer.parseInt(crypt.decrypt(v)) : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        final String v = delegate.getString(crypt.encrypt(key), null);
        return v != null ? Long.parseLong(crypt.decrypt(v)) : defValue;
    }

    @Override
    public String getString(String key, String defValue) {
        final String v = delegate.getString(crypt.encrypt(key), null);
        return v != null ? crypt.decrypt(v) : defValue;
    }

    @Override
    public boolean contains(String s) {
        return delegate.contains(crypt.encrypt(s));
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        delegate.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        delegate.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public Set<String> getStringSet(String arg0, Set<String> arg1) {
        throw new UnsupportedOperationException("This method is not implemented in " + ObscuredSharedPreferences.class.getSimpleName());
    }

    public class Editor implements SharedPreferences.Editor {
        protected SharedPreferences.Editor delegate;

        public Editor() {
            this.delegate = ObscuredSharedPreferences.this.delegate.edit();
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            delegate.putString(crypt.encrypt(key), crypt.encrypt(Boolean.toString(value)));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            delegate.putString(crypt.encrypt(key), crypt.encrypt(Float.toString(value)));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            delegate.putString(crypt.encrypt(key), crypt.encrypt(Integer.toString(value)));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            delegate.putString(crypt.encrypt(key), crypt.encrypt(Long.toString(value)));
            return this;
        }

        @Override
        public Editor putString(String key, String value) {
            delegate.putString(crypt.encrypt(key), crypt.encrypt(value));
            return this;
        }

        @Override
        public void apply() {
            delegate.apply();
        }

        @Override
        public Editor clear() {
            delegate.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return delegate.commit();
        }

        @Override
        public Editor remove(String key) {
            delegate.remove(crypt.encrypt(key));
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(
                String arg0, Set<String> arg1) {
            return null;
        }
    }
}