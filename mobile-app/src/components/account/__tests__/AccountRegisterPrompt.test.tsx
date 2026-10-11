import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react-native';
import AccountRegisterPrompt from '../AccountRegisterPrompt';

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => {
            const map: Record<string, string> = {
                'account.registerPrompt': 'Register to get free readings',
                'account.registerNow': 'Register Now',
                'account.loginPrompt': 'Already have an account?',
                'account.loginNow': 'Login',
            };
            return map[key] ?? key;
        },
    }),
}));

describe('AccountRegisterPrompt', () => {
    it('renders register prompt text and button', async () => {
        const onRegister = jest.fn();
        const onLogin = jest.fn();
        await render(
            <AccountRegisterPrompt onRegister={onRegister} onLogin={onLogin} />,
        );
        expect(
            screen.getByText('Register to get free readings'),
        ).toBeTruthy();
        expect(screen.getByText('Register Now')).toBeTruthy();
    });

    it('renders login prompt text and button', async () => {
        const onRegister = jest.fn();
        const onLogin = jest.fn();
        await render(
            <AccountRegisterPrompt onRegister={onRegister} onLogin={onLogin} />,
        );
        expect(screen.getByText('Already have an account?')).toBeTruthy();
        expect(screen.getByText('Login')).toBeTruthy();
    });

    it('calls onRegister when register button pressed', async () => {
        const onRegister = jest.fn();
        const onLogin = jest.fn();
        await render(
            <AccountRegisterPrompt onRegister={onRegister} onLogin={onLogin} />,
        );
        fireEvent.press(screen.getByText('Register Now'));
        expect(onRegister).toHaveBeenCalledTimes(1);
        expect(onLogin).not.toHaveBeenCalled();
    });

    it('calls onLogin when login button pressed', async () => {
        const onRegister = jest.fn();
        const onLogin = jest.fn();
        await render(
            <AccountRegisterPrompt onRegister={onRegister} onLogin={onLogin} />,
        );
        fireEvent.press(screen.getByText('Login'));
        expect(onLogin).toHaveBeenCalledTimes(1);
        expect(onRegister).not.toHaveBeenCalled();
    });
});
