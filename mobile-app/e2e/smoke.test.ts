import { device, expect, element, by } from 'detox';

describe('Smoke test', () => {
    beforeAll(async () => {
        await device.launchApp({ newInstance: true });
    });

    it('should render the tab bar with three tabs', async () => {
        await expect(element(by.text('Trang chủ'))).toBeVisible();
        await expect(element(by.text('Ghi chú'))).toBeVisible();
        await expect(element(by.text('Tài khoản'))).toBeVisible();
    });
});
