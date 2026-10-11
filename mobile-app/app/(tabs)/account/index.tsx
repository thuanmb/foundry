import { useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, StyleSheet, ScrollView, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import Constants from 'expo-constants';
import { colors, typography, spacing } from '@/theme';
import { AccountMenuItem, AccountProfileHeader, AccountRegisterPrompt } from '@/components/account';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { selectIsAuthenticated, selectUser } from '@/redux/selectors';
import { fetchProfile, logout } from '@/redux/slices';
import { switchLanguage } from '@/i18n';

export default function AccountScreen() {
    const { t, i18n } = useTranslation();
    const router = useRouter();
    const dispatch = useAppDispatch();
    const isAuthenticated = useAppSelector(selectIsAuthenticated);
    const user = useAppSelector(selectUser);

    const currentLang = i18n.language === 'en' ? 'en' : 'vi';

    useEffect(() => {
        if (isAuthenticated) dispatch(fetchProfile());
    }, [isAuthenticated, dispatch]);

    const openLogin = useCallback(() => router.push('/(auth)/login'), [router]);

    const handleToggleLanguage = useCallback(() => {
        void switchLanguage(currentLang === 'vi' ? 'en' : 'vi');
    }, [currentLang]);

    const handleLogout = useCallback(() => {
        Alert.alert(t('account.logoutConfirm.title'), t('account.logoutConfirm.message'), [
            { text: t('common.cancel'), style: 'cancel' },
            {
                text: t('account.logoutConfirm.confirm'),
                style: 'destructive',
                onPress: () => void dispatch(logout()),
            },
        ]);
    }, [dispatch, t]);

    const handleDeleteAccount = useCallback(() => {
        Alert.alert(t('account.deleteConfirm.title'), t('account.deleteConfirm.message'), [
            { text: t('common.cancel'), style: 'cancel' },
            {
                text: t('account.deleteConfirm.confirm'),
                style: 'destructive',
                onPress: () => router.push('/(tabs)/account/delete-account'),
            },
        ]);
    }, [router, t]);

    return (
        <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
            <AccountProfileHeader
                displayName={isAuthenticated ? user?.displayName : null}
                phoneNumber={isAuthenticated ? user?.phoneNumber : undefined}
            />

            {!isAuthenticated ? <AccountRegisterPrompt onRegister={openLogin} onLogin={openLogin} /> : null}

            <View style={styles.menu}>
                {isAuthenticated ? (
                    <AccountMenuItem
                        icon="✎"
                        label={t('account.menu.editProfile')}
                        onPress={() => router.push('/(tabs)/account/edit-profile')}
                    />
                ) : null}
                <AccountMenuItem
                    icon="🌐"
                    label={t('account.menu.language', { language: t(`account.languageName.${currentLang}`) })}
                    onPress={handleToggleLanguage}
                />
                <AccountMenuItem
                    icon="🔒"
                    label={t('account.menu.privacy')}
                    onPress={() => router.push('/(tabs)/account/privacy')}
                />
                {isAuthenticated ? (
                    <>
                        <AccountMenuItem icon="⎋" label={t('account.menu.logout')} onPress={handleLogout} />
                        <AccountMenuItem
                            icon="🗑"
                            label={t('account.menu.deleteAccount')}
                            onPress={handleDeleteAccount}
                            variant="danger"
                        />
                    </>
                ) : null}
            </View>

            <Text style={styles.version}>
                {t('account.version', { version: Constants.expoConfig?.version ?? '1.0.0' })}
            </Text>
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    flex: {
        flex: 1,
        backgroundColor: colors.background,
    },
    content: {
        paddingBottom: spacing.xxl,
        gap: spacing.lg,
    },
    menu: {
        paddingHorizontal: spacing.lg,
        gap: spacing.sm,
    },
    version: {
        ...typography.bodySmall,
        color: colors.textSubtle,
        textAlign: 'center',
    },
});
