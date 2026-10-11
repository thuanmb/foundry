import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react-native';
import OtpInput from '../OtpInput';

describe('OtpInput', () => {
    it('renders 6 input fields by default', async () => {
        await render(<OtpInput onComplete={jest.fn()} />);
        for (let i = 0; i < 6; i++) {
            expect(screen.getByTestId(`otp-input-${i}`)).toBeTruthy();
        }
    });

    it('renders custom length inputs', async () => {
        await render(<OtpInput length={4} onComplete={jest.fn()} />);
        expect(screen.getByTestId('otp-input-3')).toBeTruthy();
        expect(screen.queryByTestId('otp-input-4')).toBeNull();
    });

    it('calls onComplete when all digits are filled', async () => {
        const onComplete = jest.fn();
        await render(<OtpInput onComplete={onComplete} />);

        for (let i = 0; i < 5; i++) {
            await act(async () => {
                fireEvent.changeText(screen.getByTestId(`otp-input-${i}`), String(i + 1));
            });
        }
        // Last digit triggers onComplete
        await act(async () => {
            fireEvent.changeText(screen.getByTestId('otp-input-5'), '6');
        });

        expect(onComplete).toHaveBeenCalledWith('123456');
    });

    it('does not call onComplete when not all digits filled', async () => {
        const onComplete = jest.fn();
        await render(<OtpInput onComplete={onComplete} />);
        await act(async () => {
            fireEvent.changeText(screen.getByTestId('otp-input-0'), '1');
        });
        expect(onComplete).not.toHaveBeenCalled();
    });

    it('does not accept input when disabled', async () => {
        const onComplete = jest.fn();
        await render(<OtpInput onComplete={onComplete} disabled />);
        await act(async () => {
            fireEvent.changeText(screen.getByTestId('otp-input-0'), '1');
        });
        expect(screen.getByTestId('otp-input-0').props.value).toBe('');
    });
});
