package com.samourai.sentinel;

import android.os.Handler;

import org.bitcoinj.core.Transaction;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class EventListeners {

    public static class ListenerWeakContainer extends WeakReference<EventListener> {

        public ListenerWeakContainer(EventListener r) {
            super(r);
        }

        @Override
        public int hashCode() {
            EventListener listener = get();

            if (listener == null)
                return 0;

            return listener.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            EventListener listener = get();

            if (listener == null)
                return false;

            return listener.equals(((ListenerWeakContainer)object).get());
        }
    }

    final static Handler handler = new Handler();

    public static abstract class EventListener {

        public abstract String getDescription();

        @Override
        public int hashCode() {
            return getDescription().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            return getDescription().equals(((EventListener)object).getDescription());
        }

        public void onWalletDidChange() {
        };

        public void onCoinsSent(final Transaction tx, final long result) {
        };

        public void onCoinsReceived(final Transaction tx, final long result) {
        };

        public void onTransactionsChanged() {
        };

        public void onCurrencyChanged() {
        };

        public void onMultiAddrError() {
        };
    }

    private static final Set<ListenerWeakContainer> listeners = new HashSet<ListenerWeakContainer>();

    public static boolean addEventListener(EventListener listener) {
        synchronized (listeners) {
            if (listeners.add(new ListenerWeakContainer(listener))) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean removeEventListener(EventListener listener) {
        synchronized (listeners) {
            return listeners.remove(new ListenerWeakContainer(listener));
        }
    }

    public static void invokeOnCoinsReceived(final Transaction tx, final long result) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (final ListenerWeakContainer listener : listeners) {
                        if (listener.get() == null)
                            return;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EventListener _listener = listener.get();
                                if (_listener != null) {
                                    _listener.onCoinsReceived(tx, result);
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public static void invokeOnTransactionsChanged() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (final ListenerWeakContainer listener : listeners) {
                        if (listener.get() == null)
                            return;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EventListener _listener = listener.get();
                                if (_listener != null) {
                                    _listener.onTransactionsChanged();
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public static void invokeOnCoinsSent(final Transaction tx, final long result) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (final ListenerWeakContainer listener : listeners) {
                        if (listener.get() == null)
                            return;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EventListener _listener = listener.get();
                                if (_listener != null) {
                                    _listener.onCoinsSent(tx, result);
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public static void invokeWalletDidChange() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (final ListenerWeakContainer listener : listeners) {
                        if (listener.get() == null)
                            return;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EventListener _listener = listener.get();
                                if (_listener != null) {
                                    _listener.onWalletDidChange();
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public static void invokeCurrencyDidChange() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (final ListenerWeakContainer listener : listeners) {
                        if (listener.get() == null)
                            return;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EventListener _listener = listener.get();
                                if (_listener != null) {
                                    _listener.onCurrencyChanged();
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public static void invokeOnMultiAddrError() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (final ListenerWeakContainer listener : listeners) {
                        if (listener.get() == null)
                            return;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EventListener _listener = listener.get();
                                if (_listener != null) {
                                    _listener.onMultiAddrError();
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }
}
