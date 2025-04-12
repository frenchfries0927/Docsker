// 'use client';
//
// import { Header } from './Header';
// import { useState, useRef, useEffect } from 'react';
// import { useDraggable } from '@/components/useDraggable';
// import Image from 'next/image';
// import { useSession } from 'next-auth/react';
//
// interface MenuData {
//     id: string;
//     title: string;
//     subtitle: string;
//     menu1: string;
//     menuOpen: boolean;
// }
//
// interface MenuItem {
//     id: string;
//     title: string;
//     menu2: string;
//     menu1: string;
// }
//
// interface OrderModalState {
//     isOpen: boolean;
//     menuIndex: number | null;
//     count: number;
// }
//
// interface Order {
//     productId: string;
//     count: number;
// }
//
// const formatDateToKST = (date: Date): string => {
//     return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
// };
//
// const formatDate = (date: Date): string => {
//     const dayOfWeek = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'][date.getDay()];
//     const day = date.getDate();
//     const diff = Math.ceil((date.getTime() - new Date().getTime()) / (1000 * 3600 * 24));
//     const dayLabel = diff === 0 ? '오늘' : diff + '일 후';
//     return `${dayOfWeek}\n${day < 10 ? '0' + day : day}\n${dayLabel}`;
// };
//
// const generateDates = (): Date[] => {
//     const today = new Date();
//     return Array.from({ length: 14 }, (_, i) => {
//         const date = new Date(today);
//         date.setDate(today.getDate() + i);
//         return date;
//     });
// };
//
// export default function OrderPage() {
//     const session = useSession();
//     const [selectedDate, setSelectedDate] = useState<Date>(new Date());
//     const [timeRemaining, setTimeRemaining] = useState<string>('');
//     const [menuData, setMenuData] = useState<MenuData[]>([
//         { id: '', title: '', subtitle: '', menu1: '', menuOpen: false },
//         { id: '', title: '', subtitle: '', menu1: '', menuOpen: false },
//         { id: '', title: '', subtitle: '', menu1: '', menuOpen: false },
//     ]);
//     // selectedProductIds 제거
//     const [menuStatus, setMenuStatus] = useState<'success' | 'closed' | 'no_menu' | null>(null);
//     const [selectedOrders, setSelectedOrders] = useState<Order[]>([]);
//     const [isModalOpen, setIsModalOpen] = useState(false);
//     const [isLoading, setIsLoading] = useState(true);
//     const [isSubmitting, setIsSubmitting] = useState(false);
//     const [clickedButton, setClickedButton] = useState<number | null>(null);
//
//     // 여러 모달 상태를 하나로 통합
//     const [orderModal, setOrderModal] = useState<OrderModalState>({
//         isOpen: false,
//         menuIndex: null,
//         count: 1,
//     });
//
//     // 나머지 useRef 등은 그대로 유지
//     const containerRef = useRef<HTMLDivElement>(null);
//     const events = useDraggable<HTMLDivElement>(containerRef);
//     let isDragging = false;
//
//     const fetchMenuItems = async (selectedDate: Date, setMenuData: (data: MenuData[]) => void) => {
//         try {
//             const formattedDate = formatDateToKST(selectedDate);
//             const response = await fetch('/api/product/list', {
//                 method: 'POST',
//                 headers: {
//                     'Content-Type': 'application/json',
//                 },
//                 body: JSON.stringify(formattedDate),
//             });
//
//             if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
//
//             const result = await response.json();
//
//             if (result.status === 'success') {
//                 const newMenuData = result.data.map((item: MenuItem) => ({
//                     id: item.id,
//                     title: item.title,
//                     subtitle: item.menu2,
//                     menu1: item.menu1,
//                     menuOpen: false,
//                 }));
//                 setMenuData(newMenuData);
//             } else {
//                 setMenuData([]);
//                 // 상태를 저장할 state를 추가
//                 setMenuStatus(result.status); // 'closed' 또는 'no_menu'
//             }
//         } catch (error) {
//             console.error('Error fetching menu items:', error);
//         }
//     };
//
//     useEffect(() => {
//         const fetchData = async () => {
//             setIsLoading(true);
//             try {
//                 await fetchMenuItems(selectedDate, setMenuData);
//
//                 if (session.data?.user?.id) {
//                     const response = await fetch('/api/product/check-selection', {
//                         method: 'POST',
//                         headers: {
//                             'Content-Type': 'application/json',
//                         },
//                         credentials: 'include',
//                         body: JSON.stringify({
//                             date: formatDateToKST(selectedDate),
//                         }),
//                     });
//
//                     const data = await response.json();
//                     if (data.selectedProducts) {
//                         setSelectedOrders(data.selectedProducts);
//                         // selectedProductIds 설정 제거
//                     }
//                 }
//             } catch (error) {
//                 console.error('Error fetching data:', error);
//             } finally {
//                 setIsLoading(false);
//             }
//         };
//
//         fetchData();
//     }, [selectedDate, session.data?.user?.id]);
//
//     useEffect(() => {
//         const calculateTimeRemaining = () => {
//             const targetDate = new Date(selectedDate);
//             const dayOfWeek = targetDate.getDay();
//             let deadline;
//
//             // 토요일, 일요일, 월요일인 경우
//             if (dayOfWeek === 0 || dayOfWeek === 6 || dayOfWeek === 1) {
//                 deadline = new Date(targetDate);
//                 // 토요일이면 1일 전, 일요일이면 2일 전, 월요일이면 3일 전
//                 const daysToSubtract = dayOfWeek === 6 ? 1 : dayOfWeek === 0 ? 2 : 3;
//                 deadline.setDate(deadline.getDate() - daysToSubtract); // 금요일로 설정
//                 deadline.setHours(15, 0, 0, 0);
//             } else {
//                 // 그 외 평일인 경우
//                 deadline = new Date(targetDate);
//                 deadline.setDate(deadline.getDate() - 1);
//                 deadline.setHours(15, 0, 0, 0);
//             }
//
//             const now = new Date();
//             const timeDiff = deadline.getTime() - now.getTime();
//
//             if (timeDiff < 0) {
//                 setTimeRemaining('메뉴 선택이 마감되었습니다.');
//                 return;
//             }
//
//             const daysRemaining = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
//             const hoursRemaining = Math.floor((timeDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
//             setTimeRemaining(` ${daysRemaining}일 ${hoursRemaining}시간 후 `);
//         };
//
//         calculateTimeRemaining();
//         const intervalId = setInterval(calculateTimeRemaining, 60000);
//         return () => clearInterval(intervalId);
//     }, [selectedDate]);
//
//     useEffect(() => {
//         const dates = generateDates();
//         const todayIndex = dates.findIndex(date => formatDate(date).includes('오늘'));
//         if (todayIndex !== -1) {
//             setClickedButton(todayIndex);
//             setSelectedDate(dates[todayIndex]);
//         }
//     }, []);
//
//     const checkDeadline = (date: Date) => {
//         const targetDate = new Date(date);
//         const dayOfWeek = targetDate.getDay(); // 0: 일요일, 1: 월요일, ..., 6: 토요일
//
//         // 토요일, 일요일, 월요일인 경우, 그 주의 금요일 15시가 마감시간
//         if (dayOfWeek === 0 || dayOfWeek === 6 || dayOfWeek === 1) {
//             const fridayDate = new Date(date);
//             // 토요일이면 1일 전, 일요일이면 2일 전, 월요일이면 3일 전
//             const daysToSubtract = dayOfWeek === 6 ? 1 : dayOfWeek === 0 ? 2 : 3;
//             fridayDate.setDate(fridayDate.getDate() - daysToSubtract);
//             fridayDate.setHours(15, 0, 0, 0);
//             return new Date() > fridayDate;
//         }
//
//         // 그 외 평일인 경우는 전날 15시가 마감시간
//         targetDate.setDate(targetDate.getDate() - 1);
//         targetDate.setHours(15, 0, 0, 0);
//         return new Date() > targetDate;
//     };
//
//     const handleMouseDown = (event: React.MouseEvent<HTMLDivElement>) => {
//         isDragging = false;
//         events.onMouseDown(event);
//     };
//
//     const handleMouseMove = (event: React.MouseEvent<HTMLDivElement>) => {
//         isDragging = true;
//         events.onMouseMove(event);
//     };
//
//     const handleMouseUp = (event: React.MouseEvent<HTMLDivElement>, index: number, date: Date) => {
//         events.onMouseUp(event);
//         if (!isDragging) {
//             setClickedButton(index);
//             setSelectedDate(date);
//         }
//     };
//
//     const toggleMenu = (index: number) => {
//         setMenuData(prevData => prevData.map((data, i) => (i === index ? { ...data, menuOpen: !data.menuOpen } : data)));
//     };
//
//     const sendMenuSelection = async (menuIndex: number) => {
//         const isDeadlinePassed = checkDeadline(selectedDate);
//         if (isSubmitting || isDeadlinePassed) {
//             setIsModalOpen(true);
//             return;
//         }
//
//         try {
//             setIsSubmitting(true);
//             const selectedMenuData = menuData[menuIndex];
//             const response = await fetch('/api/product/select', {
//                 method: 'POST',
//                 headers: {
//                     'Content-Type': 'application/json',
//                 },
//                 credentials: 'include',
//                 body: JSON.stringify({
//                     productId: selectedMenuData.id,
//                     title: selectedMenuData.title,
//                     subtitle: selectedMenuData.subtitle,
//                     date: formatDateToKST(selectedDate),
//                 }),
//             });
//
//             if (!response.ok) throw new Error('Failed to select menu');
//
//             if (selectedMenuData.title === '미주문') {
//                 setSelectedOrders([{ productId: selectedMenuData.id, count: 1 }]);
//             } else {
//                 setSelectedOrders(prev => {
//                     const hasNoOrder = prev.some(order => menuData.find(item => item.id === order.productId)?.title === '미주문');
//
//                     if (hasNoOrder) {
//                         return [{ productId: selectedMenuData.id, count: 1 }];
//                     }
//
//                     if (prev.some(order => order.productId === selectedMenuData.id)) {
//                         return prev;
//                     }
//
//                     return [...prev, { productId: selectedMenuData.id, count: 1 }];
//                 });
//             }
//         } catch (error) {
//             console.error('Error sending menu selection:', error);
//         } finally {
//             setIsSubmitting(false);
//         }
//     };
//
//     const cancelOrder = async (productId: string) => {
//         if (isSubmitting) return;
//
//         const isDeadlinePassed = checkDeadline(selectedDate);
//         if (isDeadlinePassed) {
//             setIsModalOpen(true);
//             return;
//         }
//
//         try {
//             setIsSubmitting(true);
//             const response = await fetch('/api/product/select', {
//                 method: 'PUT',
//                 headers: {
//                     'Content-Type': 'application/json',
//                 },
//                 credentials: 'include',
//                 body: JSON.stringify({
//                     productId,
//                     type: 'delete',
//                 }),
//             });
//
//             if (!response.ok) throw new Error('Failed to cancel order');
//
//             // selectedProductIds 관련 코드 제거하고 단순화
//             setSelectedOrders(prev => prev.filter(order => order.productId !== productId));
//         } catch (error) {
//             console.error('Error canceling order:', error);
//         } finally {
//             setIsSubmitting(false);
//         }
//     };
//
//     const closeModal = () => setIsModalOpen(false);
//
//     const updateOrderCount = async (newCount: number) => {
//         if (orderModal.menuIndex === null) return;
//
//         const isDeadlinePassed = checkDeadline(selectedDate);
//         if (isDeadlinePassed) {
//             setIsModalOpen(true);
//             setOrderModal({ isOpen: false, menuIndex: null, count: 1 });
//             return;
//         }
//
//         const selectedMenuData = menuData[orderModal.menuIndex];
//         try {
//             setIsSubmitting(true);
//             const response = await fetch('/api/product/select', {
//                 method: 'PUT',
//                 headers: {
//                     'Content-Type': 'application/json',
//                 },
//                 credentials: 'include',
//                 body: JSON.stringify({
//                     productId: selectedMenuData.id,
//                     type: 'cnt',
//                     cnt: newCount,
//                 }),
//             });
//
//             if (!response.ok) throw new Error('Failed to update count');
//
//             setSelectedOrders(prev => {
//                 const orderIndex = prev.findIndex(order => order.productId === selectedMenuData.id);
//                 if (orderIndex === -1) return prev;
//
//                 const newOrders = [...prev];
//                 newOrders[orderIndex] = { ...newOrders[orderIndex], count: newCount };
//                 return newOrders;
//             });
//
//             // 모달 상태 리셋
//             setOrderModal({ isOpen: false, menuIndex: null, count: 1 });
//         } catch (error) {
//             console.error('Error updating count:', error);
//         } finally {
//             setIsSubmitting(false);
//         }
//     };
//
//     return (
//         <div className="mb-8 flex min-h-screen w-full flex-col bg-gray-100 pb-20 font-semibold">
//             <div className="fixed z-10 w-full bg-white">
//                 <Header />
//             </div>
//
//             <div className="w-full border-b border-gray-300 bg-white pl-3 pr-3 pt-16">
//                 <div
//                     className="flex cursor-grab snap-x snap-mandatory gap-2 overflow-hidden overflow-x-auto scrollbar-hide"
//                     ref={containerRef}
//                     style={{ scrollBehavior: 'smooth' }}>
//                     {generateDates().map((date, index) => (
//                         <div
//                             key={index}
//                             className={`mb-4 flex w-14 shrink-0 flex-col items-center justify-between rounded-lg bg-gray-100 py-1 hover:cursor-pointer ${
//                                 clickedButton === index ? 'bg-orange-200 font-bold text-black' : 'text-gray-400'
//                             }`}
//                             style={{
//                                 border: `${clickedButton === index ? '3px solid #FB923C' : '3px solid #D1D5DB'}`,
//                                 scrollBehavior: 'smooth',
//                             }}
//                             onMouseDown={handleMouseDown}
//                             onMouseMove={handleMouseMove}
//                             onMouseUp={e => handleMouseUp(e, index, date)}
//                             onMouseLeave={events.onMouseLeave}>
//                             <div className="flex flex-col items-center">
//                                 <span className="text-xs">{formatDate(date).split('\n')[0]}</span>
//                                 <span className="text-2xl">{formatDate(date).split('\n')[1]}</span>
//                                 <span className="text-xs">{formatDate(date).split('\n')[2]}</span>
//                             </div>
//                         </div>
//                     ))}
//                 </div>
//             </div>
//
//             {!isLoading && menuData.length === 0 && (
//                 <div className="flex h-[60vh] flex-col items-center justify-center">
//                     <Image src="/menu/smile.png" alt="loading" width={50} height={50} className="mb-4" />
//                     <p className="text-lg font-medium text-gray-600">
//                         {menuStatus === 'closed' ? '오늘은 쉬는 날이에요!' : '아직 메뉴가 준비중이에요!'}
//                     </p>
//                 </div>
//             )}
//
//             {!isLoading && menuData.length > 0 && (
//                 <div className="z-0 mb-16 mt-6 flex w-[88%] flex-col place-content-center gap-4 self-center">
//                     <div className="text-center text-lg text-black">
//                         {timeRemaining === '메뉴 선택이 마감되었습니다.' ? (
//                             timeRemaining
//                         ) : (
//                             <>
//                                 메뉴 선택
//                                 <span className="text-lg text-orange-500">{timeRemaining}</span>
//                                 마감됩니다.
//                             </>
//                         )}
//                     </div>
//
//                     {menuData.slice(0, -1).map((data, index) => {
//                         const isDeadlinePassed = checkDeadline(selectedDate);
//                         const isSelected = selectedOrders.some(order => order.productId === data.id);
//                         const currentOrder = selectedOrders.find(order => order.productId === data.id);
//
//                         return (
//                             <div
//                                 key={index}
//                                 className={`flex w-full flex-col rounded-2xl border-2 px-3 py-3 text-black shadow-md transition-all duration-300 ${
//                                     isSelected ? 'border-orange-400' : 'border-white'
//                                 } ${data.menuOpen ? 'h-auto' : 'h-30'} bg-white`}>
//                                 <div className="flex min-w-0 flex-wrap sm:flex-nowrap">
//                                     <Image src={`/menu/menu${index}.png`} alt="menu" width={107} height={103} />
//                                     <div className="ml-4 flex min-w-0 flex-1 flex-col justify-between">
//                                         <div className="flex flex-col">
//                                             <span className="truncate text-xl font-semibold tracking-tighter">{data.title}</span>
//                                             <span className="truncate text-sm font-medium tracking-tighter text-gray-500">
//                         {data.subtitle}
//                       </span>
//                                         </div>
//                                         <div className="flex flex-col">
//                                             {isSelected && (
//                                                 <span className="text-[11px] text-orange-400">
//                           {isDeadlinePassed
//                               ? `*주문 ${currentOrder?.count || 1}건 확정`
//                               : `*현재 ${currentOrder?.count || 1}건 대기중`}
//                         </span>
//                                             )}
//                                             <div className="flex gap-1">
//                                                 <button
//                                                     className={`w-20 rounded-lg py-1 text-sm font-medium transition-all duration-200 ${
//                                                         isSelected
//                                                             ? 'bg-orange-400 text-white'
//                                                             : isDeadlinePassed
//                                                                 ? 'cursor-not-allowed bg-gray-300'
//                                                                 : 'bg-orange-100 hover:bg-orange-200'
//                                                     }`}
//                                                     onClick={e => {
//                                                         e.stopPropagation();
//                                                         if (!isDeadlinePassed && !isSubmitting) {
//                                                             if (isSelected) {
//                                                                 setOrderModal({
//                                                                     isOpen: true,
//                                                                     menuIndex: index,
//                                                                     count: currentOrder?.count || 1,
//                                                                 });
//                                                             } else {
//                                                                 sendMenuSelection(index);
//                                                             }
//                                                         }
//                                                     }}
//                                                     disabled={isDeadlinePassed && !isSelected}>
//                                                     {isSelected
//                                                         ? isDeadlinePassed
//                                                             ? '주문확정'
//                                                             : '추가주문'
//                                                         : isDeadlinePassed
//                                                             ? '예약불가'
//                                                             : '주문예약'}
//                                                 </button>
//                                                 {isSelected && !isDeadlinePassed && (
//                                                     <button
//                                                         className="w-14 rounded-lg bg-orange-100 py-1 text-sm font-medium hover:bg-orange-200"
//                                                         onClick={async e => {
//                                                             e.stopPropagation();
//                                                             if (!isSubmitting) {
//                                                                 await cancelOrder(data.id);
//                                                             }
//                                                         }}>
//                                                         취소
//                                                     </button>
//                                                 )}
//                                             </div>
//                                         </div>
//                                     </div>
//                                     <Image
//                                         src="/menu/down.png"
//                                         alt="arrow"
//                                         className="mr-2 mt-2 shrink-0 cursor-pointer self-start"
//                                         width={15}
//                                         height={15}
//                                         onClick={e => {
//                                             e.stopPropagation();
//                                             toggleMenu(index);
//                                         }}
//                                     />
//                                 </div>
//                                 {data.menuOpen && (
//                                     <div className="mb-2 ml-14 mr-14 mt-4 flex justify-center rounded-xl bg-white p-2 shadow-md">
//                                         <Image src={data.menu1} alt={`Menu ${index + 1}`} width={500} height={300} />
//                                     </div>
//                                 )}
//                             </div>
//                         );
//                     })}
//
//                     <div
//                         className={`flex w-full cursor-pointer items-center justify-center gap-3 rounded-2xl bg-white px-4 py-6 shadow-md transition-all duration-200 ${
//                             selectedOrders.some(order => menuData.find(item => item.id === order.productId)?.title === '미주문')
//                                 ? 'border-[3px] border-orange-400 text-orange-500'
//                                 : 'text-black'
//                         }`}
//                         onClick={() => {
//                             if (isSubmitting) return;
//                             const menuIndex = menuData.findIndex(item => item.title === '미주문');
//                             if (menuIndex !== -1) {
//                                 sendMenuSelection(menuIndex);
//                             }
//                         }}>
//                         <Image
//                             src={
//                                 selectedOrders.some(order => menuData.find(item => item.id === order.productId)?.title === '미주문')
//                                     ? '/menu/smile2.png'
//                                     : '/menu/smile.png'
//                             }
//                             alt="lunchpost"
//                             className="right-3 top-4 z-0 flex"
//                             width={24}
//                             height={24}
//                         />
//                         <span className="isolate font-medium">오늘은 메뉴를 선택을 안할래요</span>
//                     </div>
//                 </div>
//             )}
//
//             {/* Count Selection Modal */}
//             {orderModal.isOpen && (
//                 <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
//                     <div className="w-3/4 rounded-lg bg-white p-6 shadow-lg md:w-96">
//                         <p className="mb-4 text-center text-lg text-black">주문 수량을 선택해주세요</p>
//                         <div className="mb-4 flex items-center justify-center gap-4">
//                             <button
//                                 onClick={() => setOrderModal(prev => ({ ...prev, count: Math.max(1, prev.count - 1) }))}
//                                 className="rounded bg-orange-100 px-4 py-2 font-bold text-black hover:bg-orange-200">
//                                 -
//                             </button>
//                             <span className="text-xl font-bold">{orderModal.count}</span>
//                             <button
//                                 onClick={() => setOrderModal(prev => ({ ...prev, count: Math.min(50, prev.count + 1) }))}
//                                 className="rounded bg-orange-100 px-4 py-2 font-bold text-black hover:bg-orange-200">
//                                 +
//                             </button>
//                         </div>
//                         <div className="flex gap-2">
//                             <button
//                                 onClick={() => setOrderModal({ isOpen: false, menuIndex: null, count: 1 })}
//                                 className="flex-1 rounded bg-gray-200 px-4 py-2 font-bold text-gray-700">
//                                 취소
//                             </button>
//                             <button
//                                 onClick={() => updateOrderCount(orderModal.count)}
//                                 className="flex-1 rounded bg-orange-500 px-4 py-2 font-bold text-white">
//                                 확인
//                             </button>
//                         </div>
//                     </div>
//                 </div>
//             )}
//
//             <div
//                 className={`fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 ${
//                     isModalOpen ? '' : 'hidden'
//                 }`}>
//                 <div className="w-3/4 rounded-lg bg-white p-6 shadow-lg md:w-96">
//                     <p className="mb-4 text-center text-lg text-black">메뉴 선택이 마감되었습니다.</p>
//                     <button onClick={closeModal} className="w-full rounded bg-orange-500 px-4 py-2 font-bold text-white">
//                         확인
//                     </button>
//                 </div>
//             </div>
//         </div>
//     );
// }
